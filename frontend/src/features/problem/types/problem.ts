export interface SampleTestCase {
    stdin: any;
    expected_output: any;
    explanation?: string;
}

export interface ProblemDetail {
    id: string;
    status: 'PUBLISHED' | 'PENDING_TEST_CASES'; 
    slug: string;
    title: string;
    description: string;
    constraints: string;
    points: number;
    timeLimitMs: number;
    memoryLimitKb: number;
    createdAt: string;
    sampleTestCases: SampleTestCase[];
}

export interface SubmissionRequest {
    problemId: string;
    language: string;
    code: string;
    matchId: string | null;
}

export interface SubmissionSummary {
    id: string;
    status: string;
    language: string;
    createdAt: string;
}

export interface PaginatedSubmissionResponse {
    submissions: SubmissionSummary[];
    currentPage: number;
    totalPages: number;
    totalItems: number;
}

// Added based on your SubmissionDetailsDTO backend class
export interface SubmissionDetails {
    id: string;
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