import type { ProblemDetail } from '../../problem/types/problem';

// --- Types for Creating & Joining a Match ---

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

// This matches your backend's LiveMatchStateDTO
export interface LiveMatchState {
    matchId: string;
    problemId: string;
    playerOneId: number;
    playerTwoId: number;
    playerOnePenalties: number;
    playerTwoPenalties: number;
    playerOneFinishTime: string | null; // Instants are sent as ISO strings
    playerTwoFinishTime: string | null;
    startedAt: string;
    durationInMinutes: number;
}

// This is the main data structure for the arena page
export interface ArenaData {
    liveState: LiveMatchState;
    problemDetails: ProblemDetail;
    playerOneUsername: string;
    playerTwoUsername: string;
}

// This corresponds to your backend's PlayerResultDTO
export interface PlayerResult {
    userId: number;
    username: string;
    score: number;
    finishTime: string | null;
    penalties: number;
}

// This now correctly matches your detailed backend MatchResultDTO
export interface MatchResult {
    matchId: string;
    problemId: string;
    startedAt: string;
    endedAt: string;
    winnerId: number | null;
    outcome: string;
    winnerUsername: string | null;
    playerOne: PlayerResult;
    playerTwo: PlayerResult;
    winningSubmissionId: string | null;
}


// --- WebSocket Event Payload Interfaces ---

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

// A union type to handle any possible message from the match topic
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