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
