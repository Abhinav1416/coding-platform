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
} from "../types/auth";


export const register = async (
  payload: RegisterPayload
): Promise<RegisterResponse> => {
  const { data } = await api.post<RegisterResponse>("/register", payload);
  return data;
};

export const toggle2FA = async (
  payload: Toggle2FAPayload
): Promise<Toggle2FAResponse> => {
  const { data } = await api.post<Toggle2FAResponse>("/2fa/toggle", payload);
  return data;
};

export const verifyEmail = async (
  payload: VerifyEmailPayload
): Promise<VerifyEmailResponse> => {
  const { data } = await api.put<VerifyEmailResponse>(
    "/validate-email-verification-token",
    payload
  );
  return data;
};

export const login = async (
  credentials: LoginCredentials
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>('/login', credentials);
  return response.data;
};

export const verify2fa = async (
  payload: Verify2faPayload
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>('/verify-2fa', payload);
  return response.data;
};

export const resendVerificationToken = async (
  payload: ResendTokenPayload
): Promise<{ message: string }> => {
  const response = await api.post('/send-email-verification-token', payload);
  return response.data;
};

export const sendPasswordResetToken = async (
  data: SendPasswordResetTokenRequest
): Promise<void> => {
  await api.post("/send-password-reset-token", data);
};

export const resetPassword = async (
  data: PasswordResetRequest
): Promise<void> => {
  await api.put("/reset-password", data);
};
