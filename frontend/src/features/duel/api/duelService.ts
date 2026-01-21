import api from '../../../core/api/api';
import type { CreateDuelRequest, DuelState, JoinDuelRequest } from '../types';


const BASE_URL = '/duels'; 

export const duelService = {
  createDuel: async (data: CreateDuelRequest): Promise<{ duelId: string; roomCode: string; status: string }> => {
    const response = await api.post(`${BASE_URL}/create`, data);
    return response.data;
  },

  joinDuel: async (roomCode: string, handle: string): Promise<DuelState> => {
    const payload: JoinDuelRequest = { handle };
    const response = await api.post(`${BASE_URL}/join/${roomCode}`, payload);
    return response.data;
  },

  getDuelState: async (duelId: string): Promise<DuelState> => {
    const response = await api.get(`${BASE_URL}/${duelId}`);
    return response.data;
  },

  getDuelHistory: async (duelId: string) => {
    const response = await api.get(`${BASE_URL}/history/${duelId}`);
    return response.data;
  }
};