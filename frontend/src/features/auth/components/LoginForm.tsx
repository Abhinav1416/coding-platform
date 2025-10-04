import { Link } from 'react-router-dom';
import type { AuthResponse } from "../types/auth";
import { useLogin } from '../services/useLogin';



interface Props {
  onAuthenticated: (data: AuthResponse) => void;
  theme: 'light' | 'dark';
}

const LoginForm = ({ onAuthenticated, theme }: Props) => {
  const {
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
  } = useLogin(onAuthenticated);

  const inputClasses =
    theme === 'dark'
      ? 'bg-slate-700 text-white border-slate-600 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500'
      : 'bg-slate-50 text-slate-900 border-slate-300 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500';
  const textColor = theme === 'dark' ? 'text-slate-300' : 'text-slate-600';

  if (step === 'verify-2fa') {
    return (
      <div className="w-full">
        <div className="text-center mb-6">
          <h2 className={`text-2xl font-bold ${theme === 'dark' ? 'text-white' : 'text-slate-800'}`}>Enter 2FA Code</h2>
          <p className={`mt-2 text-sm ${textColor}`}>A verification code was sent to your email.</p>
        </div>
        <form onSubmit={handleVerifySubmit} className="space-y-4">
          {error && <div className="text-red-500 text-sm text-center">{error}</div>}
          <div>
            <label htmlFor="token" className={`block text-sm font-medium mb-1 ${textColor}`}>Verification Code</label>
            <input id="token" type="text" value={token} onChange={(e) => setToken(e.target.value)} placeholder="Enter code" className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`} required disabled={loading}/>
          </div>
          <button type="submit" className="w-full bg-indigo-600 text-white font-bold p-3 rounded-md hover:bg-indigo-700 transition-colors duration-300 disabled:opacity-50" disabled={loading}>
            {loading ? 'Verifying...' : 'Verify'}
          </button>
        </form>
      </div>
    );
  }

  return (
    <form onSubmit={handleLoginSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm">{error}</div>}
      <div>
        <label htmlFor="email" className={`block text-sm font-medium mb-1 ${textColor}`}>Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`} required disabled={loading}/>
      </div>
      <div>
        <div className="flex justify-between items-center">
          <label htmlFor="password" className={`block text-sm font-medium mb-1 ${textColor}`}>Password</label>
          <Link to="/forgot-password" className="text-sm text-indigo-500 hover:text-indigo-400">Forgot Password?</Link>
        </div>
        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`} required disabled={loading}/>
      </div>
      <button type="submit" className="w-full bg-indigo-600 text-white font-bold p-3 rounded-md hover:bg-indigo-700 transition-colors duration-300 disabled:opacity-50" disabled={loading}>
        {loading ? 'Signing In...' : 'Sign In'}
      </button>


      <p className="text-center text-sm text-slate-500 dark:text-slate-400">
        Don't have an account?{' '}
        <Link to="/register" className="font-semibold text-indigo-500 hover:text-indigo-400">
          Sign up
        </Link>
      </p>

      
    </form>
  );
};

export default LoginForm;
