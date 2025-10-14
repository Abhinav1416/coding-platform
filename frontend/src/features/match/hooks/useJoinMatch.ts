import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { joinMatch } from '../services/matchService';
import type { JoinMatchRequest, JoinMatchResponse } from '../types/match';

export const useJoinMatch = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const joinMatchMutation = async (data: JoinMatchRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const response: JoinMatchResponse = await joinMatch(data);
      console.log('Successfully joined match. Navigating to lobby with matchId:', response.matchId);
      navigate(`/match/lobby/${response.matchId}`);

    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to join match. The code may be invalid or the match has already started.';
      setError(errorMessage);
      if (window.location.search.includes('roomCode')) {
        navigate('/match/join');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return { joinMatchMutation, isLoading, error };
};