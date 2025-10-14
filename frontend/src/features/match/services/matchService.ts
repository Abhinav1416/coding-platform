import api from '../../../core/api/api';
import type { Page, PastMatch } from '../types/match';



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


export const createMatch = async (requestData: CreateMatchRequest): Promise<CreateMatchResponse> => {
  const response = await api.post<CreateMatchResponse>(API_BASE_URL, requestData);
  return response.data;
};


export const joinMatch = async (requestData: JoinMatchRequest): Promise<JoinMatchResponse> => {
  const response = await api.post<JoinMatchResponse>(`${API_BASE_URL}/join`, requestData);
  return response.data;
};


export const getMatchLobbyState = async (matchId: string): Promise<LobbyState> => {
  const response = await api.get<LobbyState>(`${API_BASE_URL}/lobby/${matchId}`);
  return response.data;
};


export const getArenaData = async (matchId: string): Promise<ArenaData> => {
    try {
        const response = await api.get<ArenaData>(`${API_BASE_URL}/${matchId}`);
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch arena data for match ${matchId}`, error);
        throw new Error("Could not load match data. It might have expired or is invalid.");
    }
};


export const getMatchResult = async (matchId: string): Promise<MatchResult> => {
  const response = await api.get<MatchResult>(`${API_BASE_URL}/${matchId}/results`);
  return response.data;
};


export const getPlayerStats = async (userId: number): Promise<UserStats> => {
    const response = await api.get<UserStats>(`${API_STATS_URL}/${userId}`);
    return response.data;
};




export const getMatchHistory = async (params: { page: number, size: number, result?: string }): Promise<Page<PastMatch>> => {
    const response = await api.get<Page<PastMatch>>(`${API_BASE_URL}/history`, {
        params: params
    });
    return response.data;
};


export const getCurrentUserStats = async (): Promise<UserStats> => {
    const response = await api.get<UserStats>(`${API_STATS_URL}/me`);
    return response.data;
};
