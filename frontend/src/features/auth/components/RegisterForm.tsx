import { Link, useNavigate } from "react-router-dom";
import { useRegister } from "../hooks/useRegister";
import VerifyTokenForm from "./VerifyTokenForm";

import React, { useState } from "react";
import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";
import { loginWithGoogle } from '../services/authService';
import { useQueryClient } from "@tanstack/react-query";

interface Props {
  onVerified: () => void;
  theme: "light" | "dark";
}

const RegisterForm = ({ onVerified, theme }: Props) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [googleError, setGoogleError] = useState<string | null>(null);

  const {
    email, setEmail, password, setPassword, enable2FA, setEnable2FA,
    step, error, loading, handleSubmit,
  } = useRegister(onVerified);

  const handleGoogleSuccess = async (credentialResponse: CredentialResponse) => {
    setIsGoogleLoading(true);
    setGoogleError(null);
    try {
      if (!credentialResponse.credential) {
        throw new Error("Google login failed: No credential returned.");
      }
      const { accessToken, refreshToken } = await loginWithGoogle(credentialResponse.credential);

      // --- THIS IS THE FIX ---
      // We must check that the accessToken exists before using it.
      if (!accessToken) {
        throw new Error("Login failed: No access token received from backend.");
      }
      // After this check, TypeScript knows 'accessToken' is a valid string.
      // ---------------------

      // No more error here
      localStorage.setItem('accessToken', accessToken);
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
      }

      await queryClient.invalidateQueries({ queryKey: ['permissions'] });

      navigate('/');
      
    } catch (err: any) {
      console.error("Failed to sign up with Google", err);
      setGoogleError(err.message || "Google sign up failed. Please try again.");
    } finally {
      setIsGoogleLoading(false);
    }
  };

  const inputClasses =
    theme === "dark"
      ? "bg-slate-700 text-white border-slate-600 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500"
      : "bg-slate-50 text-slate-900 border-slate-300 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500";

  const textColor = theme === "dark" ? "text-slate-300" : "text-slate-600";

  if (step === "verify") {
    return <VerifyTokenForm email={email} onVerified={onVerified} theme={theme} />;
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm text-center">{error}</div>}
      {googleError && <div className="text-red-500 text-sm text-center">{googleError}</div>}

      <div>
        <label htmlFor="email" className={`block text-sm font-medium mb-1 ${textColor}`}>
          Email
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading || isGoogleLoading}
        />
      </div>

      <div>
        <label htmlFor="password" className={`block text-sm font-medium mb-1 ${textColor}`}>
          Password
        </label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading || isGoogleLoading}
        />
      </div>

      <label className="flex items-center space-x-3">
        <input
          type="checkbox"
          checked={enable2FA}
          onChange={(e) => setEnable2FA(e.target.checked)}
          className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
          disabled={loading || isGoogleLoading}
        />
        <span className={`text-sm ${textColor}`}>Enable Two-Factor Authentication</span>
      </label>

      <button
        type="submit"
        className="w-full bg-indigo-600 text-white font-bold p-3 rounded-md hover:bg-indigo-700 transition-colors duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
        disabled={loading || isGoogleLoading}
      >
        {loading ? "Creating Account..." : "Create Account"}
      </button>
      
      <div className="relative flex py-2 items-center">
        <div className="flex-grow border-t border-slate-600 dark:border-slate-700"></div>
        <span className="flex-shrink mx-4 text-slate-400 dark:text-slate-500">OR</span>
        <div className="flex-grow border-t border-slate-600 dark:border-slate-700"></div>
      </div>

      <div className="flex justify-center" style={{ opacity: isGoogleLoading ? 0.7 : 1 }}>
        <GoogleLogin
          onSuccess={handleGoogleSuccess}
          onError={() => {
            setGoogleError('Google sign up failed. Please try again.');
          }}
          useOneTap={false}
          theme={theme === 'dark' ? 'filled_black' : 'outline'}
          text="continue_with"
        />
      </div>
      
      <p className="text-center text-sm text-slate-500 dark:text-slate-400">
        Already have an account?{" "}
        <Link to="/login" className="font-semibold text-indigo-500 hover:text-indigo-400">
          Sign in
        </Link>
      </p>
    </form>
  );
};

export default RegisterForm;