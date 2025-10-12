import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/layout/MainLayout';


const NotFoundPage = () => {
  const navigate = useNavigate();

  return (
    <MainLayout>
      <div className="flex flex-col items-center justify-center text-center py-16 sm:py-24">
        <div className="w-full max-w-lg">
          <h1 className="text-8xl md:text-9xl font-extrabold text-[#F97316] tracking-wider">
            404
          </h1>
          <h2 className="mt-4 text-3xl md:text-4xl font-bold text-gray-900 dark:text-white">
            Page Not Found
          </h2>
          <p className="mt-4 text-lg text-gray-600 dark:text-gray-400">
            Sorry, we couldn't find the page you’re looking for. It might have
            been moved, deleted, or you may have mistyped the URL.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="mt-8 px-8 py-3 bg-[#F97316] text-white font-bold rounded-lg shadow-md hover:bg-[#EA580C] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 transition-all duration-300 ease-in-out transform hover:scale-105"
          >
            Go Back Home
          </button>
        </div>
      </div>
    </MainLayout>
  );
};

export default NotFoundPage;
