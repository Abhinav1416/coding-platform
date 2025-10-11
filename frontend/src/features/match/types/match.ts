import type { ProblemDetail } from '../../problem/types/problem';

// --- Types for Creating & Joining a Match ---
// (No changes here)
export interface CreateMatchRequest {
  difficultyMin: number;
  difficultyMax: number;
  startDelayInMinutes: number;
  durationInMinutes: number;
}
export interface CreateMatchResponse {
  matchId: string;
  roomCode: string;
  shareableLink: string;
}
export interface JoinMatchRequest {
  roomCode: string;
}
export interface JoinMatchResponse {
  matchId: string;
  scheduledAt: string;
}

// --- Types for the Pre-Match Lobby ---
// (No changes here)
export interface LobbyState {
  matchId: string;
  playerOneId: number;
  playerOneUsername: string;
  playerTwoId: number | null;
  playerTwoUsername: string | null;
  status: 'WAITING_FOR_OPPONENT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED';
  scheduledAt: string | null;
  durationInMinutes: number;
}
export interface UserStats {
  userId: number;
  duelsPlayed: number;
  duelsWon: number;
  duelsLost: number;
  duelsDrawn: number;
}
export interface Player {
  userId: number;
  username: string;
  email: string;
  duelsPlayed: number;
  duelsWon: number;
  duelsLost: number;
  duelsDrawn: number;
}

// --- Types for the Active Match Arena ---
// (No changes here, this looks correct for its purpose)
export interface LiveMatchState {
    matchId: string;
    problemId: string;
    playerOneId: number;
    playerTwoId: number;
    playerOnePenalties: number;
    playerTwoPenalties: number;
    playerOneFinishTime: string | null;
    playerTwoFinishTime: string | null;
    startedAt: string;
    durationInMinutes: number;
}
export interface ArenaData {
    liveState: LiveMatchState;
    problemDetails: ProblemDetail;
    playerOneUsername: string;
    playerTwoUsername: string;
}

// ===================================================================
// --- CORRECTED TYPES FOR MATCH RESULTS ---
// ===================================================================

// NEW TYPE: Defines a submission object inside the result payload
export interface SubmissionSummaryInResult {
    submissionId: string;
    status: string;
    submittedAt: string;
    runtimeMs: number | null;
    memoryKb: number | null;
}

// CORRECTED TYPE: This now perfectly matches the 'playerOne' and 'playerTwo'
// objects from your Postman response for the /results endpoint.
export interface PlayerResult {
    userId: number;
    solved: boolean;
    finishTime: string | null;
    penalties: number; // This field was already here, but the surrounding fields were wrong
    effectiveTime: string | null;
    submissions: SubmissionSummaryInResult[];
    // NOTE: 'username' and 'score' are NOT included here because the API
    // does not provide them inside this specific object.
}

// UPDATED TYPE: The MatchResult now uses the corrected PlayerResult.
export interface MatchResult {
    matchId: string;
    problemId: string;
    startedAt: string;
    endedAt: string;
    winnerId: number | null;
    outcome: string;
    winnerUsername: string | null;
    playerOne: PlayerResult & { username: string, score: number }; // Merging for component compatibility
    playerTwo: PlayerResult & { username: string, score: number }; // Merging for component compatibility
    winningSubmissionId: string | null;
}

// --- WebSocket Event Payload Interfaces ---
// (No changes from here onwards)
export interface PlayerJoinedPayload {
  eventType: 'PLAYER_JOINED';
  playerTwoId: number;
}
export interface MatchStartPayload {
    eventType: 'MATCH_START';
    liveState: LiveMatchState;
    playerOneUsername: string;
    playerTwoUsername: string;
}
export interface MatchEndPayload {
    eventType: 'MATCH_END';
    result: MatchResult;
}
export interface MatchCanceledPayload {
    eventType: 'MATCH_CANCELED';
    reason: string;
}
export type MatchEvent =
    | PlayerJoinedPayload
    | MatchStartPayload
    | MatchEndPayload
    | MatchCanceledPayload;

export interface Page<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
}

export interface PastMatch {
    matchId: string;
    status: string;
    result: 'WIN' | 'LOSS' | 'DRAW' | 'CANCELED' | 'EXPIRED';
    opponentId: number | null;
    opponentUsername: string;
    problemId: string | null;
    problemTitle: string;
    endedAt: string | null;
    createdAt: string;
}