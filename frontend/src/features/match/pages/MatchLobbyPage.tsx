import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useCountdown } from '../../../core/hooks/useCountdown';
import { PlayerCard } from '../components/PlayerCard';
import type { LobbyState, Player, MatchEvent, UserStats } from '../types/match';
import { getMatchLobbyState, getPlayerStats } from '../services/matchService';
import { stompService } from '../../../core/sockets/stompClient';

const MatchLobbyPage = () => {
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();
  const [lobbyState, setLobbyState] = useState<LobbyState | null>(null);
  const [players, setPlayers] = useState<(Player | null)[]>([null, null]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const countdownTarget = lobbyState?.scheduledAt || '';
  const { minutes, seconds } = useCountdown(countdownTarget);

  const fetchFullLobbyData = useCallback(async () => {
    if (!matchId) return;
    try {
      const state = await getMatchLobbyState(matchId);
      setLobbyState(state);

      const p1StatsPromise = getPlayerStats(state.playerOneId);
      const p2StatsPromise = state.playerTwoId ? getPlayerStats(state.playerTwoId) : Promise.resolve(null);
      
      const [p1Stats, p2Stats] = await Promise.all([p1StatsPromise, p2StatsPromise]);

      // --- THIS IS THE CORRECTED LOGIC ---
      if (p1Stats) {
        // ✅ FIX: Spread the stats object first, then add the missing properties.
        const player1: Player = {
            ...p1Stats, // Contains userId and all duel stats
            username: state.playerOneUsername,
            email: '', // Placeholder as it's not needed for the card
        };
        
        const player2: Player | null = p2Stats && state.playerTwoUsername ? {
            ...p2Stats, // Contains userId and all duel stats
            username: state.playerTwoUsername,
            email: '', // Placeholder
        } : null;

        setPlayers([player1, player2]);
      }
      // --- END OF FIX ---

    } catch (err: any) {
      setError(err.message || 'Could not find the match lobby. It may have expired or is invalid.');
    } finally {
      setIsLoading(false);
    }
  }, [matchId]);

  useEffect(() => {
    fetchFullLobbyData();
    
    const subscription = stompService.subscribeToMatchUpdates(matchId!, (event: MatchEvent) => {
      switch (event.eventType) {
        case 'PLAYER_JOINED':
          fetchFullLobbyData();
          break;
        case 'MATCH_START':
          navigate(`/match/arena/${matchId}`);
          break;
        case 'MATCH_CANCELED':
          setError(`Match Canceled: ${event.reason}`);
          setTimeout(() => navigate('/home'), 5000);
          break;
      }
    });

    return () => { if (subscription) subscription.unsubscribe(); };
  }, [matchId, fetchFullLobbyData, navigate]);
  
  const [player1, player2] = players;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center">
        <Loader2 className="animate-spin text-[#F97316]" size={48} />
        <p className="mt-4 text-white">Loading Lobby...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center text-center p-4">
        <h2 className="text-3xl font-bold text-red-500 mb-4">An Error Occurred</h2>
        <p className="text-lg text-gray-300 mb-8">{error}</p>
        <p className="text-gray-500">{error.includes("Canceled") && "Redirecting you home shortly..."}</p>
      </div>
    );
  }
  
  if (!lobbyState) {
      return (
        <div className="min-h-screen bg-gray-900 flex items-center justify-center text-red-400 text-xl">
            Could not load lobby information.
        </div>
      );
  }

  return (
    <div className="min-h-screen bg-gray-900 text-gray-300 p-4 md:p-8">
      <div className="container mx-auto max-w-5xl">
        <div className="text-center mb-8">
          {lobbyState.status === 'SCHEDULED' ? (
            <>
              <p className="text-gray-400 text-xl">Match starting soon...</p>
              <h1 className="text-6xl font-bold text-white tracking-wider my-2">
                {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
              </h1>
              <p className="text-gray-500 mt-2">The arena will open automatically.</p>
            </>
          ) : (
            <h1 className="text-4xl font-bold text-[#F97316]">Waiting for Opponent</h1>
          )}
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-stretch">
          <PlayerCard player={player1} />
          <PlayerCard player={player2} />
        </div>
      </div>
    </div>
  );
};

export default MatchLobbyPage;