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