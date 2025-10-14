export interface SampleTestCase {
    stdin: string;
    expected_output: string;
    explanation?: string;
}


export interface ProblemDetail {
    id: string;
    slug: string;
    status: 'PUBLISHED' | 'PENDING_TEST_CASES';
    createdAt: string;
    title: string;
    description: string;
    points: number;
    timeLimitMs: number;
    memoryLimitKb: number;
    sampleTestCases: SampleTestCase[];
    constraints: string; 
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