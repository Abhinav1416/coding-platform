import { useState, useEffect, useCallback } from 'react';
import { stompService } from '../../../core/sockets/stompClient';
import type { MatchEvent, MatchResult } from '../types/match';


type MatchStatus = 'LOADING' | 'WAITING_FOR_START' | 'IN_PROGRESS' | 'COMPLETED';

export const useMatchEvents = (matchId: string | undefined) => {
    const [matchState, setMatchState] = useState<MatchStatus>('WAITING_FOR_START');
    const [playerUsernames, setPlayerUsernames] = useState({ p1: 'Player 1', p2: 'Waiting...' });
    const [matchResult, setMatchResult] = useState<MatchResult | null>(null);

    const handleMatchEvent = useCallback((event: MatchEvent) => {
        console.log("MATCH EVENT RECEIVED:", event);
        switch (event.eventType) {
            case 'MATCH_START':
                setPlayerUsernames({ p1: event.playerOneUsername, p2: event.playerTwoUsername });
                setMatchState('IN_PROGRESS');
                break;
            case 'MATCH_END':
                setMatchResult(event.result);
                setMatchState('COMPLETED');
                break;
            case 'MATCH_CANCELED':
                setMatchResult({
                    outcome: event.reason,
                    winnerId: null
                } as MatchResult);
                setMatchState('COMPLETED');
                break;
        }
    }, []);

    useEffect(() => {
        if (!matchId) return;

        stompService.connect();
        const subscription = stompService.subscribeToMatchUpdates(matchId, handleMatchEvent);

        return () => {
            if (subscription && typeof subscription.unsubscribe === 'function') {
                subscription.unsubscribe();
            }
            stompService.disconnect();
        };
    }, [matchId, handleMatchEvent]);
    
    return { matchState, playerUsernames, matchResult };
};