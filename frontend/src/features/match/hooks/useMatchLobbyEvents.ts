import { useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { stompService } from '../../../core/sockets/stompClient';
import type { MatchEvent, PlayerJoinedPayload } from '../types/match';

interface UseMatchLobbyEventsProps {
  matchId: string | undefined;
  onPlayerJoined: (payload: PlayerJoinedPayload) => void;
}


export const useMatchLobbyEvents = ({ matchId, onPlayerJoined }: UseMatchLobbyEventsProps) => {
    const navigate = useNavigate();

    const handleLobbyEvent = useCallback((event: MatchEvent) => {
        console.log("LOBBY EVENT RECEIVED:", event);

        if (event.eventType === 'MATCH_START') {
            console.log(`Match starting! Navigating to arena for match: ${matchId}`);
            navigate(`/match/arena/${matchId}`);
        } else if (event.eventType === 'PLAYER_JOINED') {
            onPlayerJoined(event);
        }
    }, [matchId, navigate, onPlayerJoined]);

    useEffect(() => {
        if (!matchId) return;

        stompService.connect();
        stompService.subscribeToMatchUpdates(matchId, handleLobbyEvent);

        return () => {
            stompService.disconnect();
        };
    }, [matchId, handleLobbyEvent]);
};