import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../../core/context/ThemeContext';

const Navbar = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();
  const isAuthenticated = !!localStorage.getItem('accessToken');

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    navigate('/login');
    window.location.reload(); 
  };

  return (
    <header className="sticky top-0 z-50 p-4 border-b border-white/10">
      <div className="container mx-auto flex justify-between items-center">
        
        {/* Left Side: Brand */}
        <Link to={isAuthenticated ? "/home" : "/login"} className="text-2xl font-bold text-[#F97316]">
          CodeDuel
        </Link>

        {/* Center: Nav Links (now always visible) */}
        {isAuthenticated && (
          <div className="flex gap-8">
            <Link to="/problems" className="text-gray-300 hover:text-[#F97316] transition-colors">Problems</Link>
            <Link to="/matches" className="text-gray-300 hover:text-[#F97316] transition-colors">Matches</Link>
          </div>
        )}

        {/* Right Side: Auth & Theme */}
        <div className="flex items-center gap-4">
          <button onClick={toggleTheme} className="p-2 rounded-full hover:bg-white/10">
             {theme === 'dark' ? '☀️' : '🌙'}
          </button>
          
          {isAuthenticated ? (
            // User is Logged In
            <div className="flex items-center gap-4">
              <button onClick={() => navigate('/profile')} className="hover:text-[#F97316]">Profile</button>
              <button onClick={handleLogout} className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-md transition-colors">
                Logout
              </button>
            </div>
          ) : (
            // User is Logged Out
            <div className="flex items-center gap-2">
              <button onClick={() => navigate('/login')} className="hover:text-[#F97316] transition-colors py-2 px-4">
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