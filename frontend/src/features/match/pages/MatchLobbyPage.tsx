import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';

import { useCountdown } from '../../../core/hooks/useCountdown';
import { PlayerCard } from '../components/PlayerCard';
import type { LobbyState, Player, MatchEvent } from '../types/match';
import { getMatchLobbyState, getPlayerDetails } from '../services/matchService';
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

      const p1Promise = getPlayerDetails(state.playerOneId);
      const p2Promise = state.playerTwoId ? getPlayerDetails(state.playerTwoId) : Promise.resolve(null);
      
      const [player1, player2] = await Promise.all([p1Promise, p2Promise]);
      setPlayers([player1, player2]);

    } catch (err) {
      setError('Could not find the match lobby. It may have expired or is invalid.');
    } finally {
      setIsLoading(false);
    }
  }, [matchId]);

  useEffect(() => {
    fetchFullLobbyData();
    
    const subscription = stompService.subscribe(`/topic/match/${matchId}`, (message) => {
      const event: MatchEvent = JSON.parse(message.body);

      // Handle all possible events from the server
      switch (event.eventType) {
        case 'PLAYER_JOINED':
          console.log("Opponent joined! Re-fetching lobby state.");
          fetchFullLobbyData();
          break;
        
        case 'MATCH_START':
          console.log("Match is starting! Navigating to arena...");
          navigate(`/match/arena/${matchId}`);
          break;

        case 'MATCH_CANCELED':
          console.log(`Match canceled by server. Reason: ${event.reason}`);
          setError(`Match Canceled: ${event.reason}`);
          // Redirect the user back home after a few seconds
          setTimeout(() => {
            navigate('/home');
          }, 5000); // 5-second delay
          break;
      }
    });

    return () => {
      if (subscription) {
        subscription.unsubscribe();
      }
    };
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

  // If the match was canceled, show a dedicated screen
  if (error) {
    return (
      <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center text-center p-4">
        <h2 className="text-3xl font-bold text-red-500 mb-4">Match Canceled</h2>
        <p className="text-lg text-gray-300 mb-8">{error.replace('Match Canceled: ', '')}</p>
        <p className="text-gray-500">Redirecting you home shortly...</p>
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
              <p className="text-gray-400">Match starting soon...</p>
              <h1 className="text-6xl font-bold text-white tracking-wider">{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}</h1>
              <p className="text-gray-500 mt-2">Waiting for server to begin the match.</p>
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