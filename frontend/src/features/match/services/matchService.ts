import api from '../../../core/api/api'; // Adjust this path to your api.ts file

// Import all necessary types from your match types file
import type {
  CreateMatchRequest,
  CreateMatchResponse,
  JoinMatchRequest,
  JoinMatchResponse,
  LobbyState,
  UserStats,
  ArenaData
} from '../types/match';

const API_BASE_URL = '/api/match';

/**
 * Sends a request to the backend to create a new match.
 * Used by the CreateMatchPage.
 * @param requestData The parameters for the new match.
 */
export const createMatch = async (requestData: CreateMatchRequest): Promise<CreateMatchResponse> => {
  const response = await api.post<CreateMatchResponse>(API_BASE_URL, requestData);
  return response.data;
};

/**
 * Sends a request for the current user to join an existing match using a room code.
 * Used by the JoinMatchPage.
 * @param requestData The room code for the match to join.
 */
export const joinMatch = async (requestData: JoinMatchRequest): Promise<JoinMatchResponse> => {
  const response = await api.post<JoinMatchResponse>(`${API_BASE_URL}/join`, requestData);
  return response.data;
};

/**
 * Fetches the current state of a match lobby.
 * Used by the MatchLobbyPage.
 * @param matchId The unique ID of the match.
 */
export const getMatchLobbyState = async (matchId: string): Promise<LobbyState> => {
  const response = await api.get<LobbyState>(`${API_BASE_URL}/lobby/${matchId}`);
  return response.data;
};

/**
 * Fetches the detailed data required for the match arena, including the full problem details.
 * Used by the MatchArenaPage.
 * @param matchId The unique ID of the match.
 */
export const getArenaData = async (matchId: string): Promise<ArenaData> => {
    try {
        // This endpoint matches your MatchController's getDuelState method
        const response = await api.get<ArenaData>(`${API_BASE_URL}/${matchId}`);
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch arena data for match ${matchId}`, error);
        throw new Error("Could not load match data. It might have expired or is invalid.");
    }
};

/**
 * Fetches ONLY the competitive stats for a given user.
 * Used by the MatchLobbyPage to build the PlayerCard.
 * @param userId The ID of the user to fetch stats for.
 */
export const getPlayerDetails = async (userId: number): Promise<UserStats> => {
    try {
        // This call correctly points to your /api/stats/{userId} endpoint
        const response = await api.get<UserStats>(`/api/stats/${userId}`);
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch stats for user ${userId}`, error);
        throw new Error(`Could not load stats for user ${userId}.`);
    }
};