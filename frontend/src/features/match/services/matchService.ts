import api from '../../../core/api/api'; // Adjust path if needed
import type {
  CreateMatchRequest,
  CreateMatchResponse,
  JoinMatchRequest,
  JoinMatchResponse,
  LobbyState,
  Player,
  UserStats
} from '../types/match';

const API_BASE_URL = '/api/match';

/**
 * Sends a request to the backend to create a new match.
 */
export const createMatch = async (requestData: CreateMatchRequest): Promise<CreateMatchResponse> => {
  const response = await api.post<CreateMatchResponse>(API_BASE_URL, requestData);
  return response.data;
};

/**
 * Sends a request for the current user to join an existing match.
 */
export const joinMatch = async (requestData: JoinMatchRequest): Promise<JoinMatchResponse> => {
  const response = await api.post<JoinMatchResponse>(`${API_BASE_URL}/join`, requestData);
  return response.data;
};

/**
 * Fetches the current state of a match lobby using the specific lobby endpoint.
 * @param matchId The unique ID for the match.
 */
export const getMatchLobbyState = async (matchId: string): Promise<LobbyState> => {
  const response = await api.get<LobbyState>(`${API_BASE_URL}/lobby/${matchId}`);
  return response.data;
};

/**
 * Fetches full player details by combining user info with their duel stats.
 * @param userId The ID of the user to fetch.
 */
export const getPlayerDetails = async (userId: number): Promise<Player> => {
  // This function makes two API calls in parallel for efficiency:
  // 1. Gets the user's duel stats from the real /api/stats/{userId} endpoint.
  // 2. Gets the user's basic info (like email). This part is mocked for now.
  
  const [statsResponse, userInfoMock] = await Promise.all([
    // REAL API CALL to your new stats endpoint
    api.get<UserStats>(`/api/stats/${userId}`), 

    // MOCK: Replace this with a real API call to an endpoint like `/api/users/{userId}`
    Promise.resolve({ 
        email: `user_${userId}@gmail.com`, 
        username: `user_${userId}`
    })
  ]);

  const userStats = statsResponse.data;

  // Combine the results into the complete Player object that the UI expects
  const player: Player = {
    userId: userStats.userId,
    username: userInfoMock.username,
    email: userInfoMock.email,
    duelsPlayed: userStats.duelsPlayed,
    duelsWon: userStats.duelsWon,
    duelsLost: userStats.duelsLost,
    duelsDrawn: userStats.duelsDrawn
  };

  return player;
};