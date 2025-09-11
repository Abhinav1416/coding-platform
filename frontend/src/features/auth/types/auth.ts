export interface RegisterPayload {
  email: string;
  password: string;
}

export interface RegisterResponse {
  accessToken: string | null;
  refreshToken: string | null;
  message: string;
}

export interface Toggle2FAPayload {
  email: string;
}

export interface Toggle2FAResponse {
  message: string;
}

export interface VerifyEmailPayload {
  email: string;
  token: string;
}

export interface VerifyEmailResponse {
  message: string;
}

export interface AuthResponse {
  accessToken: string | null;
  refreshToken: string | null;
  message: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface Verify2faPayload {
  email: string;
  token: string;
}

export interface ResendTokenPayload {
  email: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface SendPasswordResetTokenRequest {
  email: string;
}

export interface PasswordResetRequest {
  email: string;
  token: string;
  newPassword: string;
}