import { useState } from 'react';
import type { AuthResponse } from "../types/auth";
import { login, verify2fa } from '../services/authService';


export const useLogin = (
  onAuthenticated: (data: AuthResponse) => void
) => {
  const [step, setStep] = useState<'login' | 'verify-2fa'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await login({ email, password });
      if (response.accessToken === null) {
        setStep('verify-2fa');
      } else {
        onAuthenticated(response);
      }
    } catch (err: any) {
      setError(err.message || 'Invalid credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await verify2fa({ email, token });
      onAuthenticated(response);
    } catch (err: any) {
      setError(err.message || 'Invalid 2FA token.');
    } finally {
      setLoading(false);
    }
  };

  return {
    step,
    email,
    setEmail,
    password,
    setPassword,
    token,
    setToken,
    error,
    loading,
    handleLoginSubmit,
    handleVerifySubmit,
  };
};