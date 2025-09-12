import React, { useState, useEffect } from 'react';

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
  theme,
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

  const inputClasses =
    theme === 'dark'
      ? 'bg-slate-700 text-white border-slate-600 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500'
      : 'bg-slate-50 text-slate-900 border-slate-300 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500';
  const textColor = theme === 'dark' ? 'text-slate-300' : 'text-slate-600';
  const successColor = theme === 'dark' ? 'text-green-400' : 'text-green-600';

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <div className="text-center">
        <h2
          className={`text-2xl font-bold ${
            theme === 'dark' ? 'text-white' : 'text-slate-800'
          }`}
        >
          Reset Your Password
        </h2>
        <p className={`mt-2 text-sm ${textColor}`}>
          Enter the token sent to your email and your new password.
        </p>
      </div>

      {error && (
        <div className="text-red-500 text-sm text-center p-2 rounded-md bg-red-500/10">
          {error}
        </div>
      )}
      {success && (
        <div
          className={`${successColor} text-sm text-center p-2 rounded-md bg-green-500/10`}
        >
          {success}
        </div>
      )}

      <div>
        <label
          htmlFor="token"
          className={`block text-sm font-medium mb-1 ${textColor}`}
        >
          Verification Code
        </label>
        <input
          id="token"
          type="text"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          placeholder="Enter code"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading}
        />
      </div>

      <div>
        <label
          htmlFor="newPassword"
          className={`block text-sm font-medium mb-1 ${textColor}`}
        >
          New Password
        </label>
        <input
          id="newPassword"
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="••••••••"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading}
        />
      </div>

      <div>
        <label
          htmlFor="confirmPassword"
          className={`block text-sm font-medium mb-1 ${textColor}`}
        >
          Confirm New Password
        </label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="••••••••"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading}
        />
      </div>

      <button
        type="submit"
        className="w-full bg-indigo-600 text-white font-bold p-3 rounded-md hover:bg-indigo-700 transition-colors duration-300 disabled:opacity-50"
        disabled={loading}
      >
        {loading ? 'Resetting...' : 'Reset Password'}
      </button>

      <div className="text-center text-sm">
        <p className={`${textColor}`}>
          Didn't receive the token?{' '}
          <button
            type="button"
            onClick={handleResend}
            disabled={loading || cooldown > 0}
            className="font-semibold text-indigo-500 hover:text-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed"
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
