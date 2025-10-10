import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import RegisterForm from "../components/RegisterForm";
import ThemeToggle from "../components/ThemeToggle";
import { useTheme } from "../../../core/context/ThemeContext";
import type { AuthResponse } from "../types/auth";

const RegisterPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { theme, toggleTheme } = useTheme();

  const handleLoginSuccess = (data: AuthResponse) => {
    if (data.accessToken) {
      localStorage.setItem('accessToken', data.accessToken);
      if (data.refreshToken) {
        localStorage.setItem('refreshToken', data.refreshToken);
      }
      queryClient.invalidateQueries({ queryKey: ['permissions'] });
      navigate("/");
    } else {
      console.error("Login failed: No access token was returned after verification.");
      navigate("/login");
    }
  };

  // Define dynamic classes for theme switching
  const pageBgClass = theme === 'dark' ? 'bg-gray-900' : 'bg-slate-100';
  const cardBgClass = theme === 'dark' ? 'bg-zinc-900 border border-white/10' : 'bg-white shadow-lg';
  const headingClass = theme === 'dark' ? 'text-white' : 'text-slate-900';
  const textClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';

  return (
    <div className={`min-h-screen flex flex-col items-center justify-center p-4 transition-colors duration-300 ${pageBgClass}`}>
      <div className="absolute top-4 right-4">
        <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
      </div>
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className={`text-3xl font-bold ${headingClass}`}>
            Create Your Account
          </h1>
          <p className={`mt-2 ${textClass}`}>
            Join us and start your journey!
          </p>
        </div>
        <div className={`p-8 rounded-xl transition-colors duration-300 w-full ${cardBgClass}`}>
          <RegisterForm onLoginSuccess={handleLoginSuccess} theme={theme} />
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;

