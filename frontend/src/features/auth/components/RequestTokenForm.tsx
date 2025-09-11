import React from 'react';
import { Link } from 'react-router-dom';

interface Props {
  email: string;
  setEmail: (email: string) => void;
  onSubmit: (e: React.FormEvent) => void;
  loading: boolean;
  error: string;
  success: string;
  theme: 'light' | 'dark';
}

const RequestTokenForm = ({ email, setEmail, onSubmit, loading, error, success, theme }: Props) => {
  const inputClasses =
    theme === 'dark'
      ? 'bg-slate-700 text-white border-slate-600 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500'
      : 'bg-slate-50 text-slate-900 border-slate-300 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500';
  const textColor = theme === 'dark' ? 'text-slate-300' : 'text-slate-600';
  const successColor = theme === 'dark' ? 'text-green-400' : 'text-green-600';

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm text-center p-2 rounded-md bg-red-500/10">{error}</div>}
      {success && <div className={`${successColor} text-sm text-center p-2 rounded-md bg-green-500/10`}>{success}</div>}

      <div>
        <label htmlFor="email" className={`block text-sm font-medium mb-1 ${textColor}`}>Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`} required disabled={loading}/>
      </div>

      <button type="submit" className="w-full bg-indigo-600 text-white font-bold p-3 rounded-md hover:bg-indigo-700 transition-colors duration-300 disabled:opacity-50" disabled={loading}>
        {loading ? 'Sending...' : 'Send Password Reset Link'}
      </button>

      <p className="text-center text-sm">
        <Link to="/login" className="font-semibold text-indigo-500 hover:text-indigo-400">
          Back to Sign In
        </Link>
      </p>
    </form>
  );
};

export default RequestTokenForm;

