import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, Ban, Swords } from 'lucide-react';
import { useCountdown } from '../../../core/hooks/useCountdown';
import { PlayerCard } from '../components/PlayerCard';
import type { LobbyState, Player, MatchEvent } from '../types/match';
import { getMatchLobbyState, getPlayerStats } from '../services/matchService';
import { stompService } from '../../../core/sockets/stompClient';

const MatchLobbyPage = () => {
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();
  const [lobbyState, setLobbyState] = useState<LobbyState | null>(null);
  const [players, setPlayers] = useState<(Player | null)[]>([null, null]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellationReason, setCancellationReason] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(5);

  const countdownTarget = lobbyState?.scheduledAt || '';
  const { minutes, seconds, isFinished } = useCountdown(countdownTarget);

  const fetchFullLobbyData = useCallback(async () => {
    if (!matchId) return;
    try {
      const state = await getMatchLobbyState(matchId);
      setLobbyState(state);
      const [p1Stats, p2Stats] = await Promise.all([
        getPlayerStats(state.playerOneId),
        state.playerTwoId ? getPlayerStats(state.playerTwoId) : Promise.resolve(null)
      ]);
      if (p1Stats) {
        const player1: Player = { ...p1Stats, username: state.playerOneUsername, email: '' };
        const player2: Player | null = p2Stats && state.playerTwoUsername ? { ...p2Stats, username: state.playerTwoUsername, email: '' } : null;
        setPlayers([player1, player2]);
      }
    } catch (err: any) {
      setError(err.message || 'Could not find the match lobby. It may have expired or is invalid.');
    } finally {
      setIsLoading(false);
    }
  }, [matchId]);

  useEffect(() => {
    if (cancellationReason) {
      const timer = setInterval(() => setCountdown(prev => prev - 1), 1000);
      const redirect = setTimeout(() => navigate('/home'), 5000);
      return () => { clearInterval(timer); clearTimeout(redirect); };
    }
  }, [cancellationReason, navigate]);

  useEffect(() => {
    fetchFullLobbyData();
    const sub = stompService.subscribeToMatchUpdates(matchId!, (event: MatchEvent) => {
      switch (event.eventType) {
        case 'PLAYER_JOINED': fetchFullLobbyData(); break;
        case 'MATCH_START': navigate(`/match/arena/${matchId}`); break;
        case 'MATCH_CANCELED': setCancellationReason(event.reason || "The match was canceled."); break;
      }
    });
    return () => { if (sub) sub.unsubscribe(); };
  }, [matchId, fetchFullLobbyData, navigate]);

  const [player1, player2] = players;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex flex-col items-center justify-center text-gray-400">
        <Loader2 className="animate-spin text-[#F97316]" size={48} />
        <p className="mt-4 text-lg animate-pulse">Loading Lobby...</p>
      </div>
    );
  }

  if (cancellationReason) {
    return (
      <div className="min-h-screen bg-zinc-950 flex items-center justify-center p-4">
        <div className="bg-zinc-900 border border-yellow-500/30 rounded-xl shadow-2xl p-8 md:p-12 text-center max-w-lg">
          <Ban className="text-yellow-500 mx-auto mb-6" size={56} strokeWidth={1.5}/>
          <h2 className="text-3xl font-bold text-white mb-3">Match Canceled</h2>
          <p className="text-lg text-gray-400 mb-8">{cancellationReason}</p>
          <div className="w-full bg-zinc-800 rounded-full h-2.5">
            <div className="bg-[#F97316] h-2.5 rounded-full" style={{ width: `${countdown * 20}%`, transition: 'width 1s linear' }}></div>
          </div>
          <p className="text-gray-500 mt-4">Redirecting in {countdown}...</p>
        </div>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="min-h-screen bg-zinc-950 flex items-center justify-center p-4">
        <div className="bg-zinc-900 border border-red-500/30 rounded-xl shadow-2xl p-8 md:p-12 text-center max-w-lg">
          <h2 className="text-3xl font-bold text-red-500 mb-4">Lobby Error</h2>
          <p className="text-lg text-gray-300 mb-8">{error}</p>
          <button onClick={() => navigate('/home')} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-6 rounded-md transition-transform transform hover:scale-105">
              Go to Home
          </button>
        </div>
      </div>
    );
  }
  
  if (!lobbyState) return null;

  return (
    <div className="min-h-screen bg-zinc-950 text-gray-300 p-4 md:p-8 transition-opacity duration-500">
      <div className="container mx-auto max-w-5xl">
        <div className="text-center mb-10">
          {lobbyState.status === 'SCHEDULED' ? (
            isFinished ? (
              <>
                <p className="text-gray-400 text-2xl font-semibold animate-pulse">Finalizing Match...</p>
                <div className="my-3 flex justify-center items-center h-[96px]">
                  <Loader2 className="animate-spin text-[#F97316]" size={72} />
                </div>
                <p className="text-gray-500 mt-2">Preparing the arena, please wait a moment.</p>
              </>
            ) : (
              <>
                <p className="text-orange-400 text-2xl font-semibold">Match Starting In</p>
                <h1 className="text-8xl font-mono font-bold text-white tracking-wider my-3">
                  {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
                </h1>
                <p className="text-gray-500 mt-2">The arena will open automatically when the timer hits zero.</p>
              </>
            )
          ) : (
            <>
              <Swords className="text-[#F97316] mx-auto text-5xl mb-4 animate-pulse" />
              <h1 className="text-4xl font-bold text-white">Waiting for Opponent</h1>
              <p className="text-gray-500 mt-2">Share the room code or link to invite a player.</p>
            </>
          )}
        </div>

        {/* --- THIS IS THE CHANGE --- */}
        <div className="relative grid grid-cols-1 md:grid-cols-2 gap-16 items-stretch">
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="bg-zinc-900 text-zinc-600 font-bold text-2xl px-4 z-10">VS</span>
          </div>
          <div className="w-full h-px bg-zinc-800 absolute top-1/2 left-0"></div>
          
          <PlayerCard player={player1} />
          <PlayerCard player={player2} />
        </div>
      </div>
    </div>
  );
};

export default MatchLobbyPage;