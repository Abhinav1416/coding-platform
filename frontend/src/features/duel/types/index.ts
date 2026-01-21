export type DuelStatus = 'WAITING' | 'PENDING' | 'LIVE' | 'FINISHED' | 'CANCELLED';

export interface DuelState {
  duelId: string;
  status: DuelStatus;
  scoreboard: DuelScoreboard;
  
  player1Handle: string;
  player2Handle?: string;
  
  player1UserId: number;
  player2UserId?: number;

  problemLinks: string[];
  problemIds: string[];
  
  durationMinutes: number;
  startsInMinutes: number;
  startTime?: number;
  
  roomCode?: string;
}

export interface DuelScoreboard {
  users: Record<string, DuelUserStats>;
}

export interface DuelUserStats {
  solved: number;
  penalty: number;
  problems: Record<string, ProblemStats>;
}

export interface SubmissionData {
  verdict: string;
  timeConsumedMillis: number;
  memoryConsumedBytes: number;
  submissionTimeSeconds: number;
}

export interface CreateDuelRequest {
  handle: string;
  problemLinks: string[];
  startsInMinutes: number;
  durationMinutes: number;
}

export interface JoinDuelRequest {
  handle: string;
}

export interface ProblemStats {
  status: 'NONE' | 'OK' | 'WRONG_ANSWER' | 'COMPILATION_ERROR' | 'TIME_LIMIT_EXCEEDED' | 'MEMORY_LIMIT_EXCEEDED' | 'RUNTIME_ERROR'; 
  attempts: number;
  bestTime: number;
  penalty?: number;
  history: Record<string, SubmissionData>; 
}