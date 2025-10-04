import { useState, useEffect } from 'react';
import { resendVerificationToken, verifyEmail } from '../services/authService';

interface Props {
  email: string;
  onVerified: () => void;
  theme: 'light' | 'dark';
}

const VerifyTokenForm = ({ email, onVerified, theme }: Props) => {
  const [token, setToken] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [isTokenExpired, setIsTokenExpired] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
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

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');
    setLoading(true);

    try {
      await verifyEmail({ email, token });
      onVerified();
    } catch (err: any) {
      const errorMessage = err.message || 'Invalid verification token.';
      if (errorMessage.toLowerCase().includes('expired')) {
        setIsTokenExpired(true);
        setError('Your token has expired. Please request a new one.');
      } else {
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendToken = async () => {
    if (cooldown > 0) return;
    setError('');
    setSuccessMessage('');
    setResending(true);
    try {
      const response = await resendVerificationToken({ email });
      setSuccessMessage(response.message || 'A new token has been sent.');
      setIsTokenExpired(false);
      setToken('');
      setCooldown(30);
    } catch (err: any) {
      setError(err.message || 'Failed to resend token. Please try again.');
    } finally {
      setResending(false);
    }
  };

  const inputClasses =
    theme === 'dark'
      ? 'bg-slate-700 text-white border-slate-600'
      : 'bg-slate-50 text-slate-900 border-slate-300';
  const textColor = theme === 'dark' ? 'text-slate-300' : 'text-slate-600';

  return (
    <div className="w-full max-w-md">
      <div className="text-center mb-6">
        <h2 className={`text-2xl font-bold ${theme === 'dark' ? 'text-white' : 'text-slate-800'}`}>
          Verify Your Email
        </h2>
        <p className={`mt-2 text-sm ${textColor}`}>
          We've sent a verification token to <strong>{email}</strong>.
        </p>
      </div>

      <form onSubmit={handleVerify} className="space-y-4">
        {error && <div className="text-red-500 text-sm text-center p-2 rounded-md">{error}</div>}
        {successMessage && <div className="text-green-500 text-sm text-center p-2 rounded-md">{successMessage}</div>}

        {isTokenExpired ? (
          <button
            type="button"
            onClick={handleResendToken}
            className="w-full bg-orange-500 text-white font-bold p-3 rounded-md hover:bg-orange-600 transition-colors disabled:opacity-50"
            disabled={resending || cooldown > 0}
          >
            {resending
              ? 'Sending...'
              : cooldown > 0
              ? `Resend available in ${cooldown}s`
              : 'Resend Verification Token'}
          </button>
        ) : (
          <>
            <input
              type="text"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Enter verification token"
              className={`w-full p-3 rounded-md border focus:ring-2 focus:ring-indigo-500 ${inputClasses}`}
              required
              disabled={loading}
            />
            <button
              type="submit"
              className="w-full bg-green-600 text-white font-bold p-3 rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
              disabled={loading}
            >
              {loading ? 'Verifying...' : 'Verify Email'}
            </button>

            <button
              type="button"
              onClick={handleResendToken}
              className="w-full text-sm text-indigo-600 dark:text-indigo-400 hover:underline mt-2 disabled:opacity-50"
              disabled={resending || cooldown > 0}
            >
              {resending
                ? 'Sending...'
                : cooldown > 0
                ? `Resend available in ${cooldown}s`
                : "Didn't receive token?"}
            </button>
          </>
        )}
      </form>
    </div>
  );
};

export default VerifyTokenForm;
