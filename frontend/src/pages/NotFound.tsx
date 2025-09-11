import { useNavigate } from 'react-router-dom';
import { useTheme } from '../core/context/ThemeContext';

const NotFoundPage = () => {
  const navigate = useNavigate();
  const { theme } = useTheme();

  const containerClasses =
    theme === 'dark'
      ? 'bg-slate-900 text-slate-200'
      : 'bg-slate-100 text-slate-800';
  const textColor = theme === 'dark' ? 'text-slate-400' : 'text-slate-600';

  return (
    <div
      className={`min-h-screen flex flex-col items-center justify-center p-6 text-center transition-colors duration-300 ${containerClasses}`}
    >
      <div className="w-full max-w-lg">
        <h1 className="text-8xl md:text-9xl font-extrabold text-indigo-500 tracking-wider">
          404
        </h1>
        <h2 className="mt-4 text-3xl md:text-4xl font-bold">
          Page Not Found
        </h2>
        <p className={`mt-4 text-lg ${textColor}`}>
          Sorry, we couldn't find the page you’re looking for. It might have
          been moved, deleted, or you may have mistyped the URL.
        </p>
        <button
          onClick={() => navigate('/login')}
          className="mt-8 px-8 py-3 bg-indigo-600 text-white font-bold rounded-lg shadow-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all duration-300 ease-in-out transform hover:scale-105"
        >
          Go Back Home
        </button>
      </div>
    </div>
  );
};

export default NotFoundPage;
