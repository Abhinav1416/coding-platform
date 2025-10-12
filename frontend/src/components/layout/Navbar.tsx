import { Link, useNavigate } from 'react-router-dom';
// REMOVED: useTheme import is no longer needed
// import { useTheme } from '../../core/context/ThemeContext'; 
import { useAuth } from '../../core/hooks/useAuth';
// REMOVED: ThemeToggle import is no longer needed
// import ThemeToggle from '../../features/auth/components/ThemeToggle';

const Navbar = () => {
  const navigate = useNavigate();
  // REMOVED: useTheme hook call
  // const { theme, toggleTheme } = useTheme(); 
  const { user, logout } = useAuth(); 
  const isAuthenticated = !!user;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // This class string now determines the look of your links in the locked dark theme
  const navLinkColor = "text-gray-300 hover:text-[#F97316] transition-colors font-medium";

  return (
    // The classes here will now always render the dark mode version
    <header className="sticky top-0 z-50 p-4 bg-gray-900/80 backdrop-blur-sm border-b border-white/10">
      <div className="container mx-auto flex justify-between items-center">
        
        <Link to={isAuthenticated ? "/home" : "/"} className="text-2xl font-bold text-[#F97316]">
          CodeDuel
        </Link>

        {isAuthenticated && (
          <nav className="hidden md:flex gap-8">
            <Link to="/home" className={navLinkColor}>Home</Link>
            <Link to="/matches/history" className={navLinkColor}>Matches</Link>
          </nav>
        )}

        <div className="flex items-center gap-4">
          
          {/* REMOVED: The ThemeToggle component */}
          
          {isAuthenticated ? (
            <div className="flex items-center gap-4">
              <button onClick={() => navigate('/profile')} className={navLinkColor}>Profile</button>
              <button onClick={handleLogout} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-lg transition-colors">
                Logout
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <button onClick={() => navigate('/login')} className={`${navLinkColor} py-2 px-4`}>
                Login
              </button>
              <button onClick={() => navigate('/register')} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-lg transition-colors">
                Sign Up
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;