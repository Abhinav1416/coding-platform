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
  duelsPlayed: number; // Assuming this is for "matches"
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


export interface ArenaData {
    liveState: LiveMatchState;
    problemDetails: ProblemDetail;
    // --- ADD THESE TWO FIELDS ---
    playerOneUsername: string;
    playerTwoUsername: string;
}

export interface MatchDetails {
    id: string;
    problem: ProblemDetail;
    durationSeconds: number;
    status: 'WAITING' | 'IN_PROGRESS' | 'COMPLETED';
    playerOneId: number;
    playerTwoId: number | null;
}

export interface MatchResult {
    winnerId: number | null;
    winnerUsername?: string;
    reason: string;
}

// --- WebSocket Event Payload Interfaces ---

export interface PlayerJoinedPayload {
  eventType: 'PLAYER_JOINED';
  playerTwoId: number;
}

export interface MatchStartPayload {
    eventType: 'MATCH_START';
    liveState: LiveMatchState; // Using the full, correct LiveMatchState
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


// --- THE SINGLE, CORRECTED DISCRIMINATED UNION ---

/**
 * A union type to handle any possible message from the match topic.
 * This should be the only declaration of MatchEvent.
 */
export type MatchEvent =
    | PlayerJoinedPayload
    | MatchStartPayload
    | MatchEndPayload
    | MatchCanceledPayload;