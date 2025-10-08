import api from '../../../core/api/api';
import type { Page, PastMatch } from '../types/match';


// Import all necessary types from your match types file
import type {
  CreateMatchRequest,
  CreateMatchResponse,
  JoinMatchRequest,
  JoinMatchResponse,
  LobbyState,
  UserStats,
  ArenaData,
  MatchResult
} from '../types/match';

const API_BASE_URL = '/api/match';
const API_STATS_URL = '/api/stats';

/**
 * Sends a request to the backend to create a new match.
 */
export const createMatch = async (requestData: CreateMatchRequest): Promise<CreateMatchResponse> => {
  const response = await api.post<CreateMatchResponse>(API_BASE_URL, requestData);
  return response.data;
};

/**
 * Sends a request for the current user to join an existing match using a room code.
 */
export const joinMatch = async (requestData: JoinMatchRequest): Promise<JoinMatchResponse> => {
  const response = await api.post<JoinMatchResponse>(`${API_BASE_URL}/join`, requestData);
  return response.data;
};

/**
 * Fetches the current state of a match lobby.
 */
export const getMatchLobbyState = async (matchId: string): Promise<LobbyState> => {
  const response = await api.get<LobbyState>(`${API_BASE_URL}/lobby/${matchId}`);
  return response.data;
};

/**
 * Fetches the detailed data required for the match arena, including the full problem details.
 */
export const getArenaData = async (matchId: string): Promise<ArenaData> => {
    try {
        const response = await api.get<ArenaData>(`${API_BASE_URL}/${matchId}`);
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch arena data for match ${matchId}`, error);
        throw new Error("Could not load match data. It might have expired or is invalid.");
    }
};

/**
 * Fetches the final result of a completed match.
 * @param matchId The unique ID of the match.
 */
export const getMatchResult = async (matchId: string): Promise<MatchResult> => {
  // This URL now correctly matches your backend controller's endpoint
  const response = await api.get<MatchResult>(`${API_BASE_URL}/${matchId}/results`);
  return response.data;
};

/**
 * Fetches ONLY the competitive stats for a given user.
 * @param userId The ID of the user to fetch stats for.
 */
export const getPlayerStats = async (userId: number): Promise<UserStats> => {
    const response = await api.get<UserStats>(`${API_STATS_URL}/${userId}`);
    return response.data;
};



/**
 * Fetches the paginated match history for the current user.
 */
export const getMatchHistory = async (pageable: { page: number, size: number }): Promise<Page<PastMatch>> => {
    const response = await api.get<Page<PastMatch>>(`${API_BASE_URL}/history`, {
        params: pageable
    });
    return response.data;
};


export const getCurrentUserStats = async (): Promise<UserStats> => {
    const response = await api.get<UserStats>(`${API_STATS_URL}/me`);
    return response.data;
};
