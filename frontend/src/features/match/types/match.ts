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

// This interface matches your backend LobbyStateDTO
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

// This type is for the response from your /api/stats/{userId} endpoint
export interface UserStats {
  userId: number;
  duelsPlayed: number;
  duelsWon: number;
  duelsLost: number;
  duelsDrawn: number;
}

// A full representation of a player's details, combining user info and stats
export interface Player {
  userId: number;
  username: string;
  email: string;
  duelsPlayed: number;
  duelsWon: number;
  duelsLost: number;
  duelsDrawn: number;
}

// This represents the LiveMatchStateDTO sent with the MATCH_START event
export interface LiveMatchState {
  matchId: string;
  problemId: string;
  playerOneId: number;
  playerTwoId: number | null;
  playerOneScore: number;
  playerTwoScore: number;
  status: 'WAITING_FOR_OPPONENT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED';
  scheduledAt: string | null;
  startedAt: string | null;
  durationInMinutes: number;
  roomCode: string;
}


// --- Types for WebSocket Payloads ---

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

export interface MatchCanceledPayload {
    eventType: 'MATCH_CANCELED';
    reason: string;
}

// A union type to handle any possible message from the match topic
export type MatchEvent = PlayerJoinedPayload | MatchStartPayload | MatchCanceledPayload;