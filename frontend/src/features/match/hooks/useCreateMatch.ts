import { useState } from 'react';
import { createMatch } from '../services/matchService';
import type { CreateMatchRequest, CreateMatchResponse } from '../types/match';

export const useCreateMatch = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdMatchData, setCreatedMatchData] = useState<CreateMatchResponse | null>(null);

  const createMatchMutation = async (data: CreateMatchRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await createMatch(data);
      setCreatedMatchData(response);
    } catch (err: any) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return { createMatchMutation, isLoading, error, createdMatchData };
};