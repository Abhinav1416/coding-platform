import React, { useState, useEffect } from 'react';
import { Loader2 } from 'lucide-react';

interface Props {
  token: string;
  setToken: (token: string) => void;
  newPassword: string;
  setNewPassword: (password: string) => void;
  confirmPassword: string;
  setConfirmPassword: (password: string) => void;
  onSubmit: (e: React.FormEvent) => void;
  onResend: () => void;
  loading: boolean;
  error: string;
  success: string;
  theme: 'light' | 'dark';
}

const ResetPasswordForm = ({
  token,
  setToken,
  newPassword,
  setNewPassword,
  confirmPassword,
  setConfirmPassword,
  onSubmit,
  onResend,
  loading,
  error,
  success,
}: Props) => {
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (cooldown > 0) {
      timer = setInterval(() => {
        setCooldown((prev) => (prev > 1 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [cooldown]);

  const handleResend = () => {
    if (cooldown > 0) return;
    onResend();
    setCooldown(30);
  };

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold text-white">
          Reset Your Password
        </h2>
        <p className="mt-2 text-sm text-gray-400">
          Enter the token sent to your email and your new password.
        </p>
      </div>

      {error && (
        <div className="text-red-400 text-sm text-center p-2 rounded-md bg-red-500/10">
          {error}
        </div>
      )}
      {success && (
        <div className="text-green-400 text-sm text-center p-2 rounded-md bg-green-500/10">
          {success}
        </div>
      )}

      <div>
        <label htmlFor="token" className="block text-sm font-medium mb-1 text-gray-400">
          Verification Code
        </label>
        <input
          id="token"
          type="text"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          placeholder="Enter code"
          className="w-full p-3 rounded-md border bg-zinc-800 border-zinc-700 transition-colors duration-300 outline-none focus:ring-1 focus:ring-[#F97316] focus:border-[#F97316]"
          required
          disabled={loading}
        />
      </div>

      <div>
        <label htmlFor="newPassword" className="block text-sm font-medium mb-1 text-gray-400">
          New Password
        </label>
        <input
          id="newPassword"
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="••••••••"
          className="w-full p-3 rounded-md border bg-zinc-800 border-zinc-700 transition-colors duration-300 outline-none focus:ring-1 focus:ring-[#F97316] focus:border-[#F97316]"
          required
          disabled={loading}
        />
      </div>

      <div>
        <label htmlFor="confirmPassword" className="block text-sm font-medium mb-1 text-gray-400">
          Confirm New Password
        </label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="••••••••"
          className="w-full p-3 rounded-md border bg-zinc-800 border-zinc-700 transition-colors duration-300 outline-none focus:ring-1 focus:ring-[#F97316] focus:border-[#F97316]"
          required
          disabled={loading}
        />
      </div>

      <button
        type="submit"
        className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold p-3 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
        disabled={loading}
      >
        {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Reset Password'}
      </button>

      <div className="text-center text-sm">
        <p className="text-gray-400">
          Didn't receive the token?{' '}
          <button
            type="button"
            onClick={handleResend}
            disabled={loading || cooldown > 0}
            className="font-semibold text-[#F97316] hover:text-[#EA580C] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {cooldown > 0
              ? `Resend available in ${cooldown}s`
              : 'Resend Token'}
          </button>
        </p>
      </div>
    </form>
  );
};

export default ResetPasswordForm;