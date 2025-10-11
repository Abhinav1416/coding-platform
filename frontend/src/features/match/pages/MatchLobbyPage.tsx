import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, Ban, Swords } from 'lucide-react';
import { PlayerCard } from '../components/PlayerCard';
import type { LobbyState, Player, MatchEvent } from '../types/match';
import { getMatchLobbyState, getPlayerStats } from '../services/matchService';
import { stompService } from '../../../core/sockets/stompClient';
import { useServerTimer } from '../../../core/components/useServerTimer';

const MatchLobbyPage: React.FC = () => {
  // --- Hooks and State Initialization ---
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();

  const [lobbyState, setLobbyState] = useState<LobbyState | null>(null);
  const [players, setPlayers] = useState<(Player | null)[]>([null, null]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellationReason, setCancellationReason] = useState<string | null>(null);
  const [redirectCountdown, setRedirectCountdown] = useState(5);
  const [timerData, setTimerData] = useState<{ startTime: number | null; duration: number | null }>({ startTime: null, duration: null });

  // State for providing dynamic feedback to the user during wait times.
  const [statusMessage, setStatusMessage] = useState('Connecting to Lobby...');

  const { minutes, seconds, isFinished } = useServerTimer(timerData.startTime, timerData.duration);

  // --- Data Fetching Logic ---
  const fetchFullLobbyData = useCallback(async () => {
    if (!matchId) return;
    try {
      if (!lobbyState) {
        setStatusMessage('Synchronizing Match Data...');
      }
      
      const state: LobbyState = await getMatchLobbyState(matchId);
      setLobbyState(state);

      // Update the status message based on the fetched state
      if (state.status === 'WAITING_FOR_OPPONENT') {
        setStatusMessage('Waiting for an Opponent');
      } else if (state.status === 'SCHEDULED') {
        setStatusMessage('Match Ready! Preparing countdown...');
      }

      // If the countdown has already started, set the timer data immediately
      if (state.status === 'SCHEDULED' && state.scheduledAt) {
        setTimerData({ startTime: new Date(state.scheduledAt).getTime(), duration: state.durationInMinutes * 60 });
      }
      
      // Fetch stats for both players
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

  // --- Side Effects ---

  // Effect for handling match cancellation redirect
  useEffect(() => {
    if (cancellationReason) {
      const timer = setInterval(() => setRedirectCountdown(prev => (prev > 1 ? prev - 1 : 0)), 1000);
      const redirect = setTimeout(() => navigate('/home'), 5000);
      return () => { clearInterval(timer); clearTimeout(redirect); };
    }
  }, [cancellationReason, navigate]);

  // Main effect for initialization and WebSocket subscriptions
  useEffect(() => {
    if (!matchId) { 
      setError("Match ID is missing from the URL."); 
      setIsLoading(false);
      return; 
    }
    
    fetchFullLobbyData();
    stompService.connect();
    
    const subs = [
      stompService.subscribeToMatchUpdates(matchId, (event: MatchEvent) => {
        switch (event.eventType) {
          case 'PLAYER_JOINED': 
            fetchFullLobbyData(); 
            break;
          case 'MATCH_START': 
            navigate(`/match/arena/${matchId}`); 
            break;
          case 'MATCH_CANCELED': 
            setCancellationReason(event.reason || "The match was canceled."); 
            break;
        }
      }),
      stompService.subscribeToCountdown(matchId, (event: any) => {
        if (event.eventType && event.eventType.toUpperCase() === 'LOBBY_COUNTDOWN_STARTED') {
          const durationInSeconds = Math.round(event.payload.duration / 1000);
          setTimerData({ startTime: event.payload.startTime, duration: durationInSeconds });
        }
      })
    ];
    
    return () => { subs.forEach(sub => { if (sub) sub.unsubscribe(); }); };
  }, [matchId, fetchFullLobbyData, navigate]);


  // --- Render Logic ---

  const [player1, player2] = players;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex flex-col items-center justify-center text-gray-400">
        <Loader2 className="animate-spin text-[#F97316]" size={48} />
        <p className="mt-4 text-lg animate-pulse">{statusMessage}</p>
      </div>
    );
  }

  if (cancellationReason) {
    return (
      <div className="min-h-screen bg-zinc-950 flex flex-col items-center justify-center p-4 text-center">
        <Ban className="text-red-500" size={64} />
        <h1 className="mt-4 text-3xl font-bold text-white">Match Canceled</h1>
        <p className="mt-2 text-lg text-gray-400">{cancellationReason}</p>
        <p className="mt-6 text-sm text-gray-500">Redirecting to home in {redirectCountdown} seconds...</p>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="min-h-screen bg-zinc-950 flex flex-col items-center justify-center p-4 text-center">
        <Ban className="text-yellow-500" size={64} />
        <h1 className="mt-4 text-3xl font-bold text-white">An Error Occurred</h1>
        <p className="mt-2 text-lg text-gray-400">{error}</p>
      </div>
    );
  }
  
  if (!lobbyState) return null;

  return (
    <div className="min-h-screen bg-zinc-950 text-gray-300 p-4 md:p-8">
      <div className="container mx-auto max-w-5xl">
        <div className="text-center mb-10">
          {/* RENDER BLOCK 1: Countdown is active */}
          {lobbyState.status === 'SCHEDULED' && timerData.startTime ? (
            isFinished ? (
              <>
                <p className="text-gray-400 text-2xl font-semibold animate-pulse">Finalizing Match...</p>
                <div className="my-3 flex justify-center items-center h-[96px]"><Loader2 className="animate-spin text-[#F97316]" size={72} /></div>
                <p className="text-gray-500 mt-2">Preparing the arena...</p>
              </>
            ) : (
              <>
                <p className="text-orange-400 text-2xl font-semibold">Match Starting In</p>
                <h1 className="text-8xl font-mono font-bold text-white tracking-wider my-3">{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}</h1>
                <p className="text-gray-500 mt-2">The arena will open automatically.</p>
              </>
            )
          ) : (
            // RENDER BLOCK 2: Waiting for countdown to start
            <>
              <Swords className="text-[#F97316] mx-auto text-5xl mb-4 animate-pulse" />
              <h1 className="text-4xl font-bold text-white">{statusMessage}</h1>
              <p className="text-gray-500 mt-2">
                {lobbyState.status === 'WAITING_FOR_OPPONENT'
                  ? 'Share the room code or link to invite a player.'
                  : 'Please wait.'}
              </p>
            </>
          )}
        </div>
        
        {/* Player Cards Display */}
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