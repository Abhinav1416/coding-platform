import { useTheme } from '../../../core/context/ThemeContext';
import ForgotPasswordForm from '../components/ForgotPasswordForm';
import ThemeToggle from '../components/ThemeToggle';

const ForgotPasswordPage = () => {
  const { theme, toggleTheme } = useTheme();

  // Define dynamic classes for theme switching
  const pageBgClass = theme === 'dark' ? 'bg-gray-900' : 'bg-slate-100';
  const cardBgClass = theme === 'dark' ? 'bg-zinc-900 border border-white/10' : 'bg-white shadow-lg';
  const headingClass = theme === 'dark' ? 'text-white' : 'text-slate-900';
  const textClass = theme === 'dark' ? 'text-gray-400' : 'text-slate-600';

  return (
    <div className={`flex items-center justify-center min-h-screen ${pageBgClass} transition-colors duration-300`}>
      <div className="absolute top-4 right-4">
        <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
      </div>
      <div className={`w-full max-w-md p-8 space-y-8 rounded-xl ${cardBgClass}`}>
        <div className="text-center">
          <h1 className={`text-3xl font-bold ${headingClass}`}>Forgot Your Password?</h1>
          <p className={`mt-2 ${textClass}`}>
            No worries! Enter your email below to get a reset link.
          </p>
        </div>
        <ForgotPasswordForm theme={theme} />
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
