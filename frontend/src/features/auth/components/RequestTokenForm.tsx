import React from 'react';
import { Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';

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
  // Define dynamic classes for theme switching
  const labelClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';
  const inputClass = theme === 'dark' 
    ? 'bg-zinc-800 border-zinc-700 text-white focus:border-[#F97316] focus:ring-[#F97316]'
    : 'bg-slate-50 border-slate-300 text-slate-900 focus:border-[#F97316] focus:ring-[#F97316]';
  const successTextClass = theme === 'dark' ? 'text-green-400' : 'text-green-600';

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm text-center p-2 rounded-md bg-red-500/10">{error}</div>}
      {success && <div className={`${successTextClass} text-sm text-center p-2 rounded-md bg-green-500/10`}>{success}</div>}

      <div>
        <label htmlFor="email" className={`block text-sm font-medium mb-1 ${labelClass}`}>Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" 
          className={`w-full p-3 rounded-md border transition-colors duration-300 outline-none focus:ring-1 ${inputClass}`} 
          required disabled={loading}/>
      </div>

      <button type="submit" 
        className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold p-3 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none" 
        disabled={loading}>
        {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Send Password Reset Link'}
      </button>

      <p className="text-center text-sm">
        <Link to="/login" className="font-semibold text-[#F97316] hover:text-[#EA580C]">
          Back to Sign In
        </Link>
      </p>
    </form>
  );
};

export default RequestTokenForm;