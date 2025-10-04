import api from '../../../core/api/api';
import type { ProblemDetail, SubmissionRequest, PaginatedSubmissionResponse, SubmissionDetails } from '../types/problem';



export const getProblemBySlug = async (slug: string): Promise<ProblemDetail> => {
    const response = await api.get<ProblemDetail>(`/api/problems/${slug}`);
    return response.data;
};


export interface SubmissionResponse {
    submissionId: string;
}


export const createSubmission = async (request: SubmissionRequest): Promise<SubmissionResponse> => {
    const response = await api.post<SubmissionResponse>('/api/submissions', request);
    return response.data;
};


export const getProblemSubmissions = async (problemId: string): Promise<PaginatedSubmissionResponse> => {

    const response = await api.get<any>(`/api/submissions/problem/${problemId}`);
    


    const normalizedSubmissions = response.data.submissions.map((sub: any) => ({
        ...sub,
        language: sub.language.name, 
    }));

    return {
        ...response.data,
        submissions: normalizedSubmissions,
    };
};




export const getSubmissionDetails = async (submissionId: string): Promise<SubmissionDetails> => {
    const response = await api.get<SubmissionDetails>(`/api/submissions/${submissionId}`);
    return response.data;
};