import api from '../../../core/api/api';
import type { PaginatedProblemResponse, FetchProblemsParams } from '../types/problem';

export const fetchProblems = async (
  params: FetchProblemsParams
): Promise<PaginatedProblemResponse> => {
  const { page = 0, size = 20, tags, tagOperator = 'AND' } = params;


  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort: 'createdAt,desc',
    tagOperator,
  });

  if (tags && tags.length > 0) {
    tags.forEach(tag => queryParams.append('tags', tag));
  }
  
  const response = await api.get<PaginatedProblemResponse>(`/api/problems?${queryParams.toString()}`);
  return response.data;
};