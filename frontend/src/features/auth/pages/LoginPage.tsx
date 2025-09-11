import { useNavigate } from 'react-router-dom';
import { useTheme } from '../../../core/context/ThemeContext';
import type { AuthResponse } from "../types/auth";
import ThemeToggle from '../components/ThemeToggle';
import LoginForm from '../components/LoginForm';

const LoginPage = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  const handleAuthenticated = (data: AuthResponse) => {
    console.log('Authentication successful:', data.accessToken);
    // In a real app, you would save the tokens to your AuthContext here
    navigate('/home');
  };

  return (
    <div className={`min-h-screen flex flex-col items-center justify-center p-4 transition-colors duration-300 ${theme === 'dark' ? 'bg-slate-900' : 'bg-slate-100'}`}>
      <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className={`text-3xl font-bold ${theme === 'dark' ? 'text-white' : 'text-slate-800'}`}>Welcome Back!</h1>
          <p className={`mt-2 ${theme === 'dark' ? 'text-slate-400' : 'text-slate-600'}`}>Sign in to continue.</p>
        </div>
        <div className={`p-8 rounded-xl shadow-lg transition-colors duration-300 w-full ${theme === 'dark' ? 'bg-slate-800' : 'bg-white'}`}>
          <LoginForm onAuthenticated={handleAuthenticated} theme={theme} />
        </div>
      </div>
    </div>
  );
};

export default LoginPage;