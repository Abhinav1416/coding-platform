import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, Swords, Copy, Check, Users, Clock, AlertCircle, Slash, Scale, Gavel, Timer } from 'lucide-react';
import { toast } from 'react-hot-toast';
import MainLayout from '../../../components/layout/MainLayout';
import ErrorState from '../../../components/common/ErrorState';
import { duelService } from '../api/duelService';
import { stompService } from '../../../core/sockets/stompClient';
import type { DuelState } from '../types';
import { useAuth } from '../../../core/hooks/useAuth';



const LobbyCountdown = ({ startTime }: { startTime?: number }) => {
  const [timeLeft, setTimeLeft] = useState<string>("Syncing...");

  useEffect(() => {
    if (!startTime) {
       setTimeLeft("Ready...");
       return;
    }

    const tick = () => {
      const now = Date.now();
      const isSeconds = startTime < 100000000000; 
      const targetMs = isSeconds ? startTime * 1000 : startTime;
      const diffMs = targetMs - now;

      if (diffMs <= 0) {
        setTimeLeft("00:00");
        return;
      }

      const totalSeconds = Math.floor(diffMs / 1000);
      const m = Math.floor(totalSeconds / 60);
      const s = totalSeconds % 60;
      
      setTimeLeft(`${m}:${s.toString().padStart(2, '0')}`);
    };

    tick();
    const interval = setInterval(tick, 1000);

    return () => clearInterval(interval);
  }, [startTime]);

  return (
    <div className="bg-gray-100 dark:bg-zinc-800 rounded-lg py-4 px-8 inline-block my-6 border border-gray-200 dark:border-zinc-700 animate-in fade-in zoom-in duration-300">
      <span className="font-mono text-6xl font-bold text-gray-900 dark:text-white tracking-widest">
        {timeLeft}
      </span>
      <p className="text-xs text-gray-500 uppercase tracking-widest mt-2 font-semibold">Until Match Starts</p>
    </div>
  );
};

const DuelLobbyPage = () => {
  const { duelId } = useParams<{ duelId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [gameState, setGameState] = useState<DuelState | null>(null);
  const [joinHandle, setJoinHandle] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [copied, setCopied] = useState(false);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    if (!duelId) return;
    duelService.getDuelState(duelId)
      .then(setGameState)
      .catch(() => setIsError(true));
  }, [duelId]);

  useEffect(() => {
    if (!duelId) return;
    stompService.connect();
    const subscription = stompService.subscribeToDuel(duelId, (update: DuelState) => setGameState(update));
    return () => subscription?.unsubscribe();
  }, [duelId]);

  useEffect(() => {
    if (gameState?.status === 'LIVE') {
      navigate(`/duel/arena/${duelId}`);
    } else if (gameState?.status === 'FINISHED') {
      navigate(`/duel/result/${duelId}`);
    }
  }, [gameState?.status, duelId, navigate]);

  if (gameState?.status === 'CANCELLED') {
    return (
      <MainLayout>
        <div className="flex flex-col items-center justify-center pt-24">
          <div className="bg-red-100 dark:bg-red-900/20 p-6 rounded-full mb-6">
            <Slash className="w-12 h-12 text-red-500" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Duel Cancelled</h1>
          <p className="text-gray-500 dark:text-gray-400 mb-8 text-center max-w-md">The waiting room timed out.</p>
          <button onClick={() => navigate('/duel/create')} className="px-6 py-3 bg-[#F97316] text-white rounded-lg font-bold">Create New Duel</button>
        </div>
      </MainLayout>
    );
  }

  if (isError) return <MainLayout><ErrorState onRetry={() => window.location.reload()} /></MainLayout>;
  
  if (!gameState || !user) {
    return (
      <MainLayout>
        <div className="flex flex-col items-center justify-center pt-24 h-[60vh]">
          <Loader2 className="animate-spin text-[#F97316]" size={48} />
          <p className="mt-4 text-lg text-gray-500 animate-pulse">Synchronizing Lobby...</p>
        </div>
      </MainLayout>
    );
  }

  const isCreator = Number(user.id) === Number(gameState.player1UserId);
  const isParticipant = isCreator || (gameState.player2UserId && Number(user.id) === Number(gameState.player2UserId));

  const copyCode = () => {
    if (!gameState.roomCode) return;
    navigator.clipboard.writeText(gameState.roomCode);
    setCopied(true);
    toast.success("Room Code Copied!");
    setTimeout(() => setCopied(false), 2000);
  };

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinHandle.trim()) return;
    setIsJoining(true);
    try {
      if (gameState.roomCode) {
        await duelService.joinDuel(gameState.roomCode, joinHandle);
        toast.success("Joined successfully!");
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to join");
    } finally {
      setIsJoining(false);
    }
  };

  return (
    <MainLayout>
      <div className="container mx-auto max-w-4xl pt-16 px-4">
        <div className="text-center mb-12">
          {gameState.status === 'PENDING' ? (
             <>
                <div className="inline-flex items-center gap-2 px-4 py-1 rounded-full bg-orange-100 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400 text-sm font-bold animate-pulse mb-4">
                  <Clock size={16} /> Match Locked
                </div>
                <h1 className="text-4xl font-bold text-gray-900 dark:text-white">Get Ready!</h1>
                <LobbyCountdown startTime={gameState.startTime} />
             </>
          ) : (
             <>
                <Swords className="text-[#F97316] mx-auto text-6xl mb-6 animate-pulse" />
                <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-2">Waiting for Opponent</h1>
                <p className="text-gray-500 dark:text-gray-400 flex items-center justify-center gap-2">
                   {isCreator ? "Invite a friend to start the timer." : "Enter your handle to join."}
                </p>
             </>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-start">
            <div className="bg-white dark:bg-zinc-900 rounded-xl border border-gray-200 dark:border-zinc-700 shadow-xl overflow-hidden p-8">
                {isCreator && gameState.status === 'WAITING' && (
                    <div className="text-center">
                        <div onClick={copyCode} className="bg-gray-100 dark:bg-zinc-800 p-8 rounded-xl cursor-pointer hover:bg-gray-200 dark:hover:bg-zinc-700 transition-colors group relative border border-gray-200 dark:border-zinc-700">
                            <p className="text-xs text-gray-500 mb-2 uppercase tracking-widest font-semibold">Room Code</p>
                            <p className="text-5xl font-mono font-bold text-gray-900 dark:text-white tracking-[0.2em]">{gameState.roomCode}</p>
                            <div className="absolute top-4 right-4 text-gray-400 group-hover:text-[#F97316] transition-colors">
                                {copied ? <Check size={20} /> : <Copy size={20} />}
                            </div>
                        </div>
                        <p className="text-sm text-gray-400 mt-4">Click to copy & share</p>
                    </div>
                )}

                {(isCreator || isParticipant) && gameState.status === 'PENDING' && (
                    <div className="space-y-4">
                        <div className="flex items-center justify-between bg-gray-50 dark:bg-zinc-800 p-4 rounded-lg border border-gray-200 dark:border-zinc-700">
                            <div className="flex items-center gap-3">
                                <div className="bg-[#F97316] p-2 rounded-full text-white"><Users size={16} /></div>
                                <span className="font-bold text-gray-900 dark:text-white text-lg">{gameState.player1Handle}</span>
                            </div>
                            <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">Host</span>
                        </div>
                        <div className="flex justify-center text-gray-300 dark:text-zinc-600"><Swords size={24}/></div>
                        <div className="flex items-center justify-between bg-green-50 dark:bg-green-900/20 p-4 rounded-lg border border-green-200 dark:border-green-900/30">
                            <div className="flex items-center gap-3">
                                <div className="bg-green-600 p-2 rounded-full text-white"><Check size={16} /></div>
                                <span className="font-bold text-gray-900 dark:text-white text-lg">{gameState.player2Handle}</span>
                            </div>
                            <span className="text-xs font-bold text-green-600 dark:text-green-400 uppercase tracking-wider">Ready</span>
                        </div>
                    </div>
                )}

                {!isParticipant && gameState.status === 'WAITING' && (
                    <form onSubmit={handleJoin} className="space-y-4">
                        <div>
                            <label className="block text-sm text-gray-500 mb-1">Enter your CF Handle</label>
                            <input required className="w-full p-3 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 rounded-md outline-none focus:border-[#F97316] text-gray-900 dark:text-white transition-colors" placeholder="e.g. tourist" value={joinHandle} onChange={e => setJoinHandle(e.target.value)} />
                        </div>
                        <button type="submit" disabled={isJoining} className="w-full bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 rounded-md transition-transform active:scale-95 disabled:opacity-50">
                            {isJoining ? <Loader2 className="animate-spin mx-auto"/> : 'Join Duel'}
                        </button>
                    </form>
                )}
            </div>

            <div className="bg-gray-50 dark:bg-zinc-800/50 rounded-xl p-8 border border-gray-200 dark:border-zinc-700">
                <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
                    <Gavel size={20} className="text-[#F97316]" /> Official Duel Rules
                </h3>
                <ul className="space-y-4 text-sm text-gray-600 dark:text-gray-400">
                    <li className="flex gap-3">
                        <Scale size={20} className="shrink-0 text-blue-500" />
                        <div>
                            <strong className="text-gray-900 dark:text-white block mb-1">ICPC Scoring</strong>
                            Ranked by <span className="font-semibold text-green-600">Problems Solved</span> first. Ties are broken by <span className="font-semibold text-red-500">Time Penalty</span>.
                        </div>
                    </li>
                    <li className="flex gap-3">
                        <Timer size={20} className="shrink-0 text-orange-500" />
                        <div>
                            <strong className="text-gray-900 dark:text-white block mb-1">Penalty Calculation</strong>
                            Penalty = (Minutes to AC) + (20 mins × Wrong Submissions).
                        </div>
                    </li>
                    <li className="flex gap-3">
                        <AlertCircle size={20} className="shrink-0 text-red-500" />
                        <div>
                            <strong className="text-gray-900 dark:text-white block mb-1">Fair Play</strong>
                            Ensure your Codeforces handle matches exactly. Solutions submitted before the start time do not count.
                        </div>
                    </li>
                </ul>
            </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default DuelLobbyPage;