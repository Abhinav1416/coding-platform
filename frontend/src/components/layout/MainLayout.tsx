import React from 'react';
import Navbar from './Navbar';
import { useTheme } from '../../core/context/ThemeContext';
import { Toaster } from 'react-hot-toast';

interface MainLayoutProps {
  children: React.ReactNode;
}

const MainLayout = ({ children }: MainLayoutProps) => {
  const { theme } = useTheme();

  return (
    <div className={`min-h-screen ${theme === 'dark' ? 'bg-[#18181b] text-gray-200' : 'bg-white text-gray-800'}`}>

      <Toaster 
        position="top-center"
        reverseOrder={false}
        toastOptions={{
          className: '',
          style: {
            background: '#333',
            color: '#fff',
          },
        }}
      />
      <Navbar />
      <main className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
};

export default MainLayout;