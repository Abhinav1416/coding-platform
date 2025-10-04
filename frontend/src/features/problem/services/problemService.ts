import api from '../../../core/api/api';
import type { ProblemDetail, SubmissionRequest, PaginatedSubmissionResponse, SubmissionDetails } from '../types/problem';

export const getProblemBySlug = async (slug: string): Promise<ProblemDetail> => {
    const response = await api.get<ProblemDetail>(`/api/problems/${slug}`);
    return response.data;
};

// Interface for the POST /api/submissions response body
export interface SubmissionResponse {
    submissionId: string;
}

/**
 * Creates a new submission.
 * @param request The submission data.
 * @returns A promise that resolves to the new submission's ID.
 */
export const createSubmission = async (request: SubmissionRequest): Promise<SubmissionResponse> => {
    const response = await api.post<SubmissionResponse>('/api/submissions', request);
    return response.data;
};

/**
 * Fetches a paginated list of submissions for a given problem and user.
 * @param problemId The ID of the problem.
 * @returns A promise that resolves to a page of submission summaries.
 */
export const getProblemSubmissions = async (problemId: string): Promise<PaginatedSubmissionResponse> => {
    const response = await api.get<PaginatedSubmissionResponse>(`/api/submissions/problem/${problemId}`);
    return response.data;
};

/**
 * Fetches the full details of a single submission.
 * @param submissionId The ID of the submission.
 * @returns A promise that resolves to the submission details.
 */
export const getSubmissionDetails = async (submissionId: string): Promise<SubmissionDetails> => {
    const response = await api.get<SubmissionDetails>(`/api/submissions/${submissionId}`);
    return response.data;
};