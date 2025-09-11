import api from "../../../core/api/api";
import type {
  RegisterPayload,
  RegisterResponse,
  Toggle2FAPayload,
  Toggle2FAResponse,
  VerifyEmailPayload,
  VerifyEmailResponse,
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
