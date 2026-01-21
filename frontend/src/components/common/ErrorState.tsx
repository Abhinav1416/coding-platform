import { AlertTriangle, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

const ErrorState = ({ 
  title = "Something went wrong", 
  message = "We couldn't connect to the server. It might be down or you might be offline.", 
  onRetry 
}: ErrorStateProps) => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center h-[60vh] text-center px-4">
      <div className="bg-red-50 dark:bg-red-900/20 p-6 rounded-full mb-6">
        <AlertTriangle className="w-12 h-12 text-red-500" />
      </div>
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">{title}</h2>
      <p className="text-gray-500 dark:text-gray-400 max-w-md mb-8">{message}</p>
      
      <div className="flex gap-4">
        <button 
          onClick={() => navigate('/home')}
          className="px-6 py-2 border border-gray-300 dark:border-zinc-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-zinc-800 transition-colors"
        >
          Go Home
        </button>
        {onRetry && (
          <button 
            onClick={onRetry}
            className="px-6 py-2 bg-[#F97316] text-white rounded-lg hover:bg-[#EA580C] transition-colors flex items-center gap-2"
          >
            <RefreshCw size={18} /> Retry
          </button>
        )}
      </div>
    </div>
  );
};

export default ErrorState;