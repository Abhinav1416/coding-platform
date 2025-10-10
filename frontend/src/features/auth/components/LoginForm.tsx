import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { login } from '../services/authService';
import type { AuthResponse } from '../types/auth';

interface Props {
  onAuthenticated: (data: AuthResponse) => void;
  theme: 'light' | 'dark';
}

const LoginForm = ({ onAuthenticated, theme }: Props) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const responseData = await login({ email, password });
      onAuthenticated(responseData);
    } catch (err: any) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };
  
  // Define dynamic classes for theme switching
  const labelClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';
  const inputClass = theme === 'dark' 
    ? 'bg-zinc-800 border-zinc-700 text-white focus:border-[#F97316] focus:ring-[#F97316]'
    : 'bg-slate-50 border-slate-300 text-slate-900 focus:border-[#F97316] focus:ring-[#F97316]';
  const textClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm text-center">{error}</div>}
      
      <div>
        <label htmlFor="email" className={`block text-sm font-medium mb-2 ${labelClass}`}>Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com"
          className={`w-full p-3 border rounded-md outline-none transition-colors focus:ring-1 ${inputClass}`}
          required disabled={loading}/>
      </div>

      <div>
        <label htmlFor="password" className={`block text-sm font-medium mb-2 ${labelClass}`}>Password</label>
        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••"
          className={`w-full p-3 border rounded-md outline-none transition-colors focus:ring-1 ${inputClass}`}
          required disabled={loading}/>
      </div>

      <div className="flex items-center justify-end">
        <div className="text-sm">
          <Link to="/forgot-password" className="font-medium text-[#F97316] hover:text-[#EA580C]">
            Forgot your password?
          </Link>
        </div>
      </div>

      <div>
        <button type="submit"
          className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
          disabled={loading}>
          {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Sign In'}
        </button>
      </div>

      <p className={`text-center text-sm ${textClass}`}>Not a member?{" "}
        <Link to="/register" className="font-semibold text-[#F97316] hover:text-[#EA580C]">
          Sign up now
        </Link>
      </p>
    </form>
  );
};

export default LoginForm;