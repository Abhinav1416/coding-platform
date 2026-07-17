import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../core/hooks/useAuth';

const Navbar = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth(); 
  const isAuthenticated = !!user;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navLinkColor = "text-gray-300 hover:text-[#F97316] transition-colors font-medium";

  return (
    <header className="sticky top-0 z-50 p-4 bg-gray-900/80 backdrop-blur-sm border-b border-white/10 relative">
      <div className="container mx-auto flex justify-between items-center relative">
        
        <div className="flex items-center gap-8">
          <Link to={isAuthenticated ? "/home" : "/"} className="text-2xl font-bold text-[#F97316] shrink-0">
            CodeDuel
          </Link>

          {isAuthenticated && (
            <nav className="hidden md:flex gap-6 items-center">
              <Link to="/home" className={navLinkColor}>Home</Link>
              <Link to="/matches/history" className={navLinkColor}>Matches</Link>
            </nav>
          )}
        </div>

        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 hidden lg:flex items-center gap-3 bg-white/5 border border-white/10 rounded-full px-4 py-1.5 shadow-sm">
          <span className="relative flex h-2.5 w-2.5 shrink-0">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-orange-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-[#F97316]"></span>
          </span>
          <span className="text-sm text-gray-300 font-medium whitespace-nowrap">
            Backend paused for cost optimization.
          </span>
          <a
            href="https://www.youtube.com/watch?v=nctT-6Y0xJg"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[#F97316] hover:text-[#EA580C] text-sm font-bold transition-colors underline underline-offset-4 decoration-[#F97316]/50 hover:decoration-[#EA580C] whitespace-nowrap"
          >
            Watch AWS Demo on YouTube
          </a>
        </div>

        {/* Right Section (GitHub + Auth) */}
        <div className="flex items-center gap-5 shrink-0">
          <a
            href="https://github.com/Abhinav1416/coding-platform"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors"
            title="View Source on GitHub"
          >
            <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
              <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
            </svg>
            <span className="hidden sm:block text-sm font-medium">GitHub</span>
          </a>

          {isAuthenticated ? (
            <button onClick={handleLogout} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-lg transition-colors">
              Logout
            </button>
          ) : (
            <button onClick={() => navigate('/login')} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-lg transition-colors">
              Login / Sign Up
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;