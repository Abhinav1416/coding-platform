import { useTheme } from '../../../core/context/ThemeContext';
import ForgotPasswordForm from '../components/ForgotPasswordForm';
import ThemeToggle from '../components/ThemeToggle';

const ForgotPasswordPage = () => {
  const { theme, toggleTheme } = useTheme();

  const bgColor = theme === 'dark' ? 'bg-slate-900' : 'bg-slate-100';
  const cardColor = theme === 'dark' ? 'bg-slate-800' : 'bg-white';
  const textColor = theme === 'dark' ? 'text-white' : 'text-slate-800';

  return (
    <div className={`flex items-center justify-center min-h-screen ${bgColor} transition-colors duration-300`}>
      <div className="absolute top-4 right-4">
        <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
      </div>
      <div className={`w-full max-w-md p-8 space-y-8 rounded-xl shadow-lg ${cardColor}`}>
        <div className="text-center">
          <h1 className={`text-3xl font-bold ${textColor}`}>Forgot Your Password?</h1>
          <p className={`mt-2 ${theme === 'dark' ? 'text-slate-400' : 'text-slate-600'}`}>
            No worries! Enter your email below to get a reset link.
          </p>
        </div>
        <ForgotPasswordForm theme={theme} />
      </div>
    </div>
  );
};

export default ForgotPasswordPage;

