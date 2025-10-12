import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
// --- 1. ADDED IMPORTS FOR GOOGLE LOGIN ---
import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";
import { login, loginWithGoogle } from '../services/authService';
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

  // --- 2. ADDED STATE FOR GOOGLE LOGIN ---
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [googleError, setGoogleError] = useState<string | null>(null);

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

  // --- 3. ADDED HANDLER FOR GOOGLE SUCCESS ---
  const handleGoogleSuccess = async (credentialResponse: CredentialResponse) => {
    setIsGoogleLoading(true);
    setGoogleError(null);
    try {
      if (!credentialResponse.credential) {
        throw new Error("Google login failed: No credential returned.");
      }
      const authData = await loginWithGoogle(credentialResponse.credential);
      onAuthenticated(authData); // Call the main authentication handler
    } catch (err: any) {
      setGoogleError(err.message || "Google sign in failed. Please try again.");
    } finally {
      setIsGoogleLoading(false);
    }
  };
  
  // Define dynamic classes for theme switching
  const labelClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';
  const inputClass = theme === 'dark' 
    ? 'bg-zinc-800 border-zinc-700 text-white focus:border-[#F97316] focus:ring-[#F97316]'
    : 'bg-slate-50 border-slate-300 text-slate-900 focus:border-[#F97316] focus:ring-[#F97316]';
  const textClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';
  const dividerClass = theme === 'dark' ? 'border-zinc-700' : 'border-slate-300';
  const dividerTextClass = theme === 'dark' ? 'text-gray-500' : 'text-slate-500';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm text-center">{error}</div>}
      {googleError && <div className="text-red-500 text-sm text-center">{googleError}</div>}
      
      <div>
        <label htmlFor="email" className={`block text-sm font-medium mb-2 ${labelClass}`}>Email</label>
        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com"
          className={`w-full p-3 border rounded-md outline-none transition-colors focus:ring-1 ${inputClass}`}
          required disabled={loading || isGoogleLoading}/>
      </div>

      <div>
        <label htmlFor="password" className={`block text-sm font-medium mb-2 ${labelClass}`}>Password</label>
        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••"
          className={`w-full p-3 border rounded-md outline-none transition-colors focus:ring-1 ${inputClass}`}
          required disabled={loading || isGoogleLoading}/>
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
          disabled={loading || isGoogleLoading}>
          {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Sign In'}
        </button>
      </div>

      {/* --- 4. ADDED DIVIDER AND GOOGLE BUTTON JSX --- */}
      <div className="relative flex py-2 items-center">
        <div className={`flex-grow border-t ${dividerClass}`}></div>
        <span className={`flex-shrink mx-4 ${dividerTextClass}`}>OR</span>
        <div className={`flex-grow border-t ${dividerClass}`}></div>
      </div>

      <div className="flex justify-center" style={{ opacity: isGoogleLoading ? 0.7 : 1 }}>
        <GoogleLogin 
          onSuccess={handleGoogleSuccess} 
          onError={() => setGoogleError('Google sign in failed. Please try again.')}
          theme={theme === 'dark' ? 'filled_black' : 'outline'} 
          text="continue_with" 
        />
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
