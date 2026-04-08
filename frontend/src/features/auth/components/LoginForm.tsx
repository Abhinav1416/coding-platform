import { useState } from 'react';
import { GoogleLogin, type CredentialResponse } from "@react-oauth/google";
import { loginWithGoogle } from '../services/authService';
import type { AuthResponse } from '../types/auth';

interface Props {
  onAuthenticated: (data: AuthResponse) => void;
  theme: 'light' | 'dark';
}
 
const LoginForm = ({ onAuthenticated, theme }: Props) => {
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [googleError, setGoogleError] = useState<string | null>(null);

  const handleGoogleSuccess = async (credentialResponse: CredentialResponse) => {
    setIsGoogleLoading(true);
    setGoogleError(null);
    try {
      if (!credentialResponse.credential) {
        throw new Error("Google login failed: No credential returned.");
      }
      const authData = await loginWithGoogle(credentialResponse.credential);
      onAuthenticated(authData);
    } catch (err: any) {
      setGoogleError(err.message || "Google sign in failed. Please try again.");
    } finally {
      setIsGoogleLoading(false);
    }
  };

  return (
    <div className="space-y-6 flex flex-col items-center">
      {googleError && <div className="text-red-500 text-sm text-center">{googleError}</div>}

      <div className="flex justify-center w-full" style={{ opacity: isGoogleLoading ? 0.7 : 1 }}>
        <GoogleLogin 
          onSuccess={handleGoogleSuccess} 
          onError={() => setGoogleError('Google sign in failed. Please try again.')}
          theme={theme === 'dark' ? 'filled_black' : 'outline'} 
          text="continue_with" 
        />
      </div>
    </div>
  );
};

export default LoginForm;