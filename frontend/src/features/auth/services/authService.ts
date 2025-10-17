import api from "../../../core/api/api";
import type {
  RegisterPayload,
  RegisterResponse,
  VerifyEmailPayload,
  VerifyEmailResponse,
  AuthResponse,
  LoginCredentials,
  Verify2faPayload,
  ResendTokenPayload,
  PasswordResetRequest,
  SendPasswordResetTokenRequest,
  UserDetails,
  ChangePasswordRequest,
} from "../types/auth";

// ✅ FIXED: Removed the redundant "/api" prefix
const AUTH_BASE_PATH = "/v1/authentication";

export const register = async (
  payload: RegisterPayload
): Promise<RegisterResponse> => {
  const { data } = await api.post<RegisterResponse>(`${AUTH_BASE_PATH}/register`, payload);
  return data;
};

export const verifyEmail = async (
  payload: VerifyEmailPayload
): Promise<VerifyEmailResponse> => {
  const { data } = await api.put<VerifyEmailResponse>(
    `${AUTH_BASE_PATH}/validate-email-verification-token`,
    payload
  );
  return data;
};

export const login = async (
  credentials: LoginCredentials
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>(`${AUTH_BASE_PATH}/login`, credentials);
  return response.data;
};


export const loginWithGoogle = async (credential: string): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>(`${AUTH_BASE_PATH}/google`, { token: credential });
  return response.data;
};


export const verify2fa = async (
  payload: Verify2faPayload
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>(`${AUTH_BASE_PATH}/verify-2fa`, payload);
  return response.data;
};

export const resendVerificationToken = async (
  payload: ResendTokenPayload
): Promise<{ message: string }> => {
  const response = await api.post(`${AUTH_BASE_PATH}/send-email-verification-token`, payload);
  return response.data;
};

export const sendPasswordResetToken = async (
  data: SendPasswordResetTokenRequest
): Promise<void> => {
  await api.post(`${AUTH_BASE_PATH}/send-password-reset-token`, data);
};

export const resetPassword = async (
  data: PasswordResetRequest
): Promise<void> => {
  await api.put(`${AUTH_BASE_PATH}/reset-password`, data);
};

export const changePassword = async (
  request: ChangePasswordRequest
): Promise<void> => {
  await api.put(`${AUTH_BASE_PATH}/change-password`, request);
};

export const getCurrentUser = async (): Promise<UserDetails | null> => {
  const res = await api.get(`${AUTH_BASE_PATH}/me`);
  return res.data?.user ?? res.data ?? null;
};

export const fetchMyPermissions = async (): Promise<string[]> => {
    try {
        const response = await api.get<string[]>('/users/me/permissions');
        return response.data;
    } catch (error) {
        console.error("Failed to fetch user permissions:", error);
        return [];
    }
};


export const toggle2FA = async (): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>(`${AUTH_BASE_PATH}/2fa/toggle`);
  return response.data;
};