import React from 'react';
import Navbar from './Navbar';
import { useTheme } from '../../core/context/ThemeContext';

interface MainLayoutProps {
  children: React.ReactNode;
}

const MainLayout = ({ children }: MainLayoutProps) => {
  const { theme } = useTheme();

  return (
    // Updated to a slightly lighter, charcoal gray: #18181b
    <div className={`min-h-screen ${theme === 'dark' ? 'bg-[#18181b] text-gray-200' : 'bg-white text-gray-800'}`}>
      <Navbar />
      <main className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
};

export default MainLayout;