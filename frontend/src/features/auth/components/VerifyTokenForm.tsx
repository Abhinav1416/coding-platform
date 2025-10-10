import { useState, useEffect } from 'react';
import { Loader2 } from 'lucide-react';
import { resendVerificationToken, verifyEmail } from '../services/authService';
import type { AuthResponse } from '../types/auth';

interface Props {
  email: string;
  onLoginSuccess: (data: AuthResponse) => void;
  theme: 'light' | 'dark';
}

const VerifyTokenForm = ({ email, onLoginSuccess, theme }: Props) => {
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
      const responseData = await verifyEmail({ email, token });
      onLoginSuccess(responseData);
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
  
  // Define dynamic classes for theme switching
  const headingClass = theme === 'dark' ? 'text-white' : 'text-slate-900';
  const textClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';
  const inputClass = theme === 'dark' 
    ? 'bg-zinc-800 border-zinc-700 text-white focus:border-[#F97316] focus:ring-[#F97316]'
    : 'bg-slate-50 border-slate-300 text-slate-900 focus:border-[#F97316] focus:ring-[#F97316]';
  const successTextClass = theme === 'dark' ? 'text-green-400' : 'text-green-600';

  return (
    <div className="w-full max-w-md">
      <div className="text-center mb-6">
        <h2 className={`text-2xl font-bold ${headingClass}`}>Verify Your Email</h2>
        <p className={`mt-2 text-sm ${textClass}`}>We've sent a verification token to <strong>{email}</strong>.</p>
      </div>
      <form onSubmit={handleVerify} className="space-y-4">
        {error && <div className="text-red-500 text-sm text-center p-2 rounded-md">{error}</div>}
        {successMessage && <div className={`${successTextClass} text-sm text-center p-2 rounded-md`}>{successMessage}</div>}

        {isTokenExpired ? (
           <button type="button" onClick={handleResendToken}
            className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
            disabled={resending || cooldown > 0}>
            {resending ? <Loader2 className="h-5 w-5 animate-spin"/> : cooldown > 0 ? `Resend available in ${cooldown}s` : 'Resend Token'}
          </button>
        ) : (
          <>
            <input type="text" value={token} onChange={(e) => setToken(e.target.value)} placeholder="Enter verification token"
              className={`w-full p-3 border rounded-md outline-none transition-colors focus:ring-1 tracking-widest text-center ${inputClass}`}
              required disabled={loading}/>
            <button type="submit"
              className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
              disabled={loading}>
              {loading ? <Loader2 className="h-5 w-5 animate-spin"/> : 'Verify Email'}
            </button>
            <button type="button" onClick={handleResendToken}
              className="w-full text-sm text-[#F97316] hover:text-[#EA580C] hover:underline mt-2 disabled:opacity-50"
              disabled={resending || cooldown > 0}>
              {resending ? 'Sending...' : cooldown > 0 ? `Resend available in ${cooldown}s` : "Didn't receive token? Resend"}
            </button>
          </>
        )}
      </form>
    </div>
  );
};

export default VerifyTokenForm;