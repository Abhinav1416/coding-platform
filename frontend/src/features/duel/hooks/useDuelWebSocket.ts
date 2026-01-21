import { useEffect, useState, useRef } from 'react';
import type { StompSubscription } from '@stomp/stompjs';
import { stompService } from '../../../core/sockets/stompClient';
import type { DuelState } from '../types';

export const useDuelWebSocket = (duelId: string | undefined) => {
  const [gameState, setGameState] = useState<DuelState | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const subscriptionRef = useRef<StompSubscription | null>(null);

  useEffect(() => {
    if (!duelId) return;

    stompService.connect();
    setIsConnected(true);

    subscriptionRef.current = stompService.subscribeToDuel(duelId, (update: DuelState) => {
      console.log("WS Update:", update);
      setGameState(update);
    });

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
    };
  }, [duelId]);

  return { gameState, isConnected };
};