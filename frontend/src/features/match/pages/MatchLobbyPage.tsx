import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, Ban, Swords } from 'lucide-react';
import { PlayerCard } from '../components/PlayerCard';
import type { LobbyState, Player, MatchEvent } from '../types/match';
import { getMatchLobbyState, getPlayerStats } from '../services/matchService';
import { stompService } from '../../../core/sockets/stompClient';
import { useServerTimer } from '../../../core/components/useServerTimer';
import MainLayout from '../../../components/layout/MainLayout';


const LobbyStateLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <MainLayout>
        <div className="flex flex-col items-center justify-center text-center pt-24">
            {children}
        </div>
    </MainLayout>
);

const MatchLobbyPage: React.FC = () => {
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();
  const [lobbyState, setLobbyState] = useState<LobbyState | null>(null);
  const [players, setPlayers] = useState<(Player | null)[]>([null, null]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellationReason, setCancellationReason] = useState<string | null>(null);
  const [redirectCountdown, setRedirectCountdown] = useState(5);
  const [timerData, setTimerData] = useState<{ startTime: number | null; duration: number | null }>({ startTime: null, duration: null });
  const [statusMessage, setStatusMessage] = useState('Connecting to Lobby...');
  const { minutes, seconds, isFinished } = useServerTimer(timerData.startTime, timerData.duration);
  const fetchFullLobbyData = useCallback(async () => {
    if (!matchId) return;
    try {
      if (!lobbyState) setStatusMessage('Synchronizing Match Data...');
      const state: LobbyState = await getMatchLobbyState(matchId);
      setLobbyState(state);
      if (state.status === 'WAITING_FOR_OPPONENT') setStatusMessage('Waiting for an Opponent');
      else if (state.status === 'SCHEDULED') setStatusMessage('Match Ready! Preparing countdown...');
      if (state.status === 'SCHEDULED' && state.scheduledAt) {
        setTimerData({ startTime: new Date(state.scheduledAt).getTime(), duration: state.durationInMinutes * 60 });
      }
      const [p1Stats, p2Stats] = await Promise.all([getPlayerStats(state.playerOneId), state.playerTwoId ? getPlayerStats(state.playerTwoId) : Promise.resolve(null)]);
      if (p1Stats) {
        const player1: Player = { ...p1Stats, username: state.playerOneUsername, email: '' };
        const player2: Player | null = p2Stats && state.playerTwoUsername ? { ...p2Stats, username: state.playerTwoUsername, email: '' } : null;
        setPlayers([player1, player2]);
      }
    } catch (err: any) {
      setError(err.message || 'Could not find the match lobby.');
    } finally {
      setIsLoading(false);
    }
  }, [matchId, lobbyState]);

  useEffect(() => {
    if (cancellationReason) {
      const timer = setInterval(() => setRedirectCountdown(prev => (prev > 1 ? prev - 1 : 0)), 1000);
      const redirect = setTimeout(() => navigate('/home'), 5000);
      return () => { clearInterval(timer); clearTimeout(redirect); };
    }
  }, [cancellationReason, navigate]);

  useEffect(() => {
    if (!matchId) { setError("Match ID is missing."); setIsLoading(false); return; }
    fetchFullLobbyData();
    stompService.connect();
    const subs = [
      stompService.subscribeToMatchUpdates(matchId, (event: MatchEvent) => {
        switch (event.eventType) {
          case 'PLAYER_JOINED': fetchFullLobbyData(); break;
          case 'MATCH_START': navigate(`/match/arena/${matchId}`); break;
          case 'MATCH_CANCELED': setCancellationReason(event.reason || "The match was canceled."); break;
        }
      }),
      stompService.subscribeToCountdown(matchId, (event: any) => {
        if (event.eventType?.toUpperCase() === 'LOBBY_COUNTDOWN_STARTED') {
          const durationInSeconds = Math.round(event.payload.duration / 1000);
          setTimerData({ startTime: event.payload.startTime, duration: durationInSeconds });
        }
      })
    ];
    return () => { subs.forEach(sub => sub?.unsubscribe()); };
  }, [matchId, fetchFullLobbyData, navigate]);

  const [player1, player2] = players;

  if (isLoading) {
    return (
      <LobbyStateLayout>
        <Loader2 className="animate-spin text-[#F97316]" size={48} />
        <p className="mt-4 text-lg animate-pulse text-gray-700 dark:text-gray-400">{statusMessage}</p>
      </LobbyStateLayout>
    );
  }

  if (cancellationReason) {
    return (
      <LobbyStateLayout>
        <Ban className="text-red-500" size={64} />
        <h1 className="mt-4 text-3xl font-bold text-gray-900 dark:text-white">Match Canceled</h1>
        <p className="mt-2 text-lg text-gray-600 dark:text-gray-400">{cancellationReason}</p>
        <p className="mt-6 text-sm text-gray-500">Redirecting to home in {redirectCountdown} seconds...</p>
      </LobbyStateLayout>
    );
  }
  
  if (error) {
    return (
      <LobbyStateLayout>
        <Ban className="text-yellow-500" size={64} />
        <h1 className="mt-4 text-3xl font-bold text-gray-900 dark:text-white">An Error Occurred</h1>
        <p className="mt-2 text-lg text-gray-600 dark:text-gray-400">{error}</p>
      </LobbyStateLayout>
    );
  }
  
  if (!lobbyState) return null;

  return (
    <MainLayout>
      <div className="container mx-auto max-w-5xl">
        <div className="text-center mb-10">
          {lobbyState.status === 'SCHEDULED' && timerData.startTime ? (
            isFinished ? (
              <>
                <p className="text-2xl font-semibold animate-pulse text-gray-700 dark:text-gray-400">Finalizing Match...</p>
                <div className="my-3 flex justify-center items-center h-[96px]"><Loader2 className="animate-spin text-[#F97316]" size={72} /></div>
                <p className="text-gray-500 dark:text-gray-500 mt-2">Preparing the arena...</p>
              </>
            ) : (
              <>
                <p className="text-2xl font-semibold text-orange-500 dark:text-orange-400">Match Starting In</p>
                <h1 className="text-8xl font-mono font-bold text-gray-900 dark:text-white tracking-wider my-3">{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}</h1>
                <p className="text-gray-500 dark:text-gray-500 mt-2">The arena will open automatically.</p>
              </>
            )
          ) : (
            <>
              <Swords className="text-[#F97316] mx-auto text-5xl mb-4 animate-pulse" />
              <h1 className="text-4xl font-bold text-gray-900 dark:text-white">{statusMessage}</h1>
              <p className="text-gray-500 dark:text-gray-500 mt-2">
                {lobbyState.status === 'WAITING_FOR_OPPONENT'
                  ? 'Share the room code or link to invite a player.'
                  : 'Please wait.'}
              </p>
            </>
          )}
        </div>
        
        <div className="relative grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-16 items-stretch">
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="bg-white dark:bg-[#18181b] text-gray-400 dark:text-zinc-600 font-bold text-2xl px-4 z-10">VS</span>
            </div>
            <div className="w-full h-px bg-gray-200 dark:bg-zinc-800 absolute top-1/2 left-0"></div>
            <PlayerCard player={player1} />
            <PlayerCard player={player2} />
        </div>
      </div>
    </MainLayout>
  );
};

export default MatchLobbyPage;