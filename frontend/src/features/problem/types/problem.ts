import { z } from 'zod';


export interface Problem {
  problemId: string;
  title: string;
  slug: string;
  difficulty: number;
  tags: string[];
  createdAt: string;
}


export interface PaginatedProblemResponse {
  content: Problem[];
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  empty: boolean;
}


export interface FetchProblemsParams {
  page?: number;
  size?: number;
  tags?: string[];
  tagOperator?: 'AND' | 'OR';
}


export const problemInitiationSchema = z.object({
  title: z.string().min(5, "Title must be at least 5 characters long."),
  description: z.string().min(20, "Description must be at least 20 characters long."),
  difficulty: z.coerce.number().min(800, "Difficulty must be at least 800.").max(3000, "Difficulty cannot exceed 3000."),
  tags: z.array(z.string()).min(1, "Please select at least one tag."),
});


export type ProblemInitiationFormValues = z.infer<typeof problemInitiationSchema>;


export interface ProblemInitiationResponse {
  problemId: string;
  uploadUrl: string;
}


export type ProblemStatus = 'PUBLISHED' | 'PENDING_TEST_CASES';


export interface ProblemStatusDto {
  status: ProblemStatus;
}