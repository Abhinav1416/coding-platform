import api from '../../../core/api/api';
import type { ProblemDetail } from '../types/problem';


export const getProblemBySlug = async (slug: string): Promise<ProblemDetail> => {
    const response = await api.get<ProblemDetail>(`/api/problems/${slug}`);
    return response.data;
};