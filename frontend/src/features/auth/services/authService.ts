import api from "../../../core/api/api";
import type {
  RegisterPayload,
  RegisterResponse,
  Toggle2FAPayload,
  Toggle2FAResponse,
  VerifyEmailPayload,
  VerifyEmailResponse,
  AuthResponse,
  LoginCredentials,
  Verify2faPayload,
  ResendTokenPayload,
  PasswordResetRequest,
  SendPasswordResetTokenRequest,
  User,
} from "../types/auth";

const AUTH_BASE_PATH = "/api/v1/authentication";

export const register = async (
  payload: RegisterPayload
): Promise<RegisterResponse> => {
  const { data } = await api.post<RegisterResponse>(`${AUTH_BASE_PATH}/register`, payload);
  return data;
};

export const toggle2FA = async (
  payload: Toggle2FAPayload
): Promise<Toggle2FAResponse> => {
  const { data } = await api.post<Toggle2FAResponse>(`${AUTH_BASE_PATH}/2fa/toggle`, payload);
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

export const getCurrentUser = async (): Promise<User | null> => {
  const res = await api.get(`${AUTH_BASE_PATH}/me`);
  return res.data?.user ?? null;
};