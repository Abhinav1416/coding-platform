// src/features/problem/types/index.ts

export interface SampleTestCase {
    stdin: string;
    expected_output: string;
    explanation?: string;
}

/**
 * This is the definitive, corrected type for a problem, based on all requirements.
 */
export interface ProblemDetail {
    // Properties from your original definition
    id: string;
    slug: string;
    status: 'PUBLISHED' | 'PENDING_TEST_CASES';
    createdAt: string; // ISO Date string

    // Properties confirmed from your ProblemDetails.tsx component
    title: string;
    description: string; // Contains HTML
    points: number;
    timeLimitMs: number;
    memoryLimitKb: number;
    sampleTestCases: SampleTestCase[];
    
    // CORRECTED: Confirmed to be a single string from your component's code
    constraints: string; 

    // ADDED: Required for type unification with the match feature
    difficulty: number;
}

export interface SubmissionRequest {
    problemId: string;
    language: string;
    code: string;
    matchId: string | null;
}

export interface SubmissionSummary {
    id: string;
    matchId: string | null;
    status: string;
    language: string;
    runtimeMs: number | null;
    createdAt: string;
}

export interface PaginatedSubmissionResponse {
    submissions: SubmissionSummary[];
    currentPage: number;
    totalPages: number;
    totalItems: number;
}

export interface SubmissionDetails {
    id:string;
    problemId: string;
    matchId: string | null;
    problemTitle: string;
    problemSlug: string;
    status: string;
    language: string;
    code: string;
    runtimeMs: number | null;
    memoryKb: number | null;
    stdout: string | null;
    stderr: string | null;
    createdAt: string;
}


export interface ProblemCountResponse {
    totalCount: number;
}