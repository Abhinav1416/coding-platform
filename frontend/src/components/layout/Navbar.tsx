import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../../core/context/ThemeContext';
import { useAuth } from '../../core/hooks/useAuth';

const Navbar = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();
  const { user, logout } = useAuth(); 
  const isAuthenticated = !!user;

  const handleLogout = () => {
    logout();
  };

  const navLinkColor = "text-gray-600 dark:text-gray-300 hover:text-[#F97316] transition-colors";

  return (
    <header className="sticky top-0 z-50 p-4 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-white/10">
      <div className="container mx-auto flex justify-between items-center">
        
        <Link to={isAuthenticated ? "/home" : "/login"} className="text-2xl font-bold text-[#F97316]">
          CodeDuel
        </Link>

        {isAuthenticated && (
          <nav className="hidden md:flex gap-8">
            <Link to="/home" className={navLinkColor}>Home</Link>
            {/* ✅ "Problems" link has been removed */}
            <Link to="/matches/history" className={navLinkColor}>Matches</Link>
          </nav>
        )}

        <div className="flex items-center gap-4">
          <button onClick={toggleTheme} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-white/10">
             {theme === 'dark' ? '☀️' : '🌙'}
          </button>
          
          {isAuthenticated ? (
            <div className="flex items-center gap-4">
              <button onClick={() => navigate('/profile')} className={navLinkColor}>Profile</button>
              <button onClick={handleLogout} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-md transition-colors">
                Logout
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <button onClick={() => navigate('/login')} className={`${navLinkColor} py-2 px-4`}>
                Login
              </button>
              <button onClick={() => navigate('/register')} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-md transition-colors">
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