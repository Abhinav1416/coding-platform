import { useQuery, keepPreviousData } from '@tanstack/react-query'; // 1. Import keepPreviousData
import { fetchProblems } from '../services/problemService';
import type { FetchProblemsParams } from '../types/problem';

export const useProblems = (params: FetchProblemsParams) => {
  return useQuery({
    queryKey: ['problems', params],
    queryFn: () => fetchProblems(params),
    
    // 2. This is the correct syntax for TanStack Query v4/v5
    placeholderData: keepPreviousData, 
  });
};