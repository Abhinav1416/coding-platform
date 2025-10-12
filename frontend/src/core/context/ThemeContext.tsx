import React, { createContext, useState, useEffect, useContext, type ReactNode } from 'react';

// Define the shape of the context's value
interface ThemeContextType {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

// Create the context with a default value.
// The default is used when a component is rendered outside of the provider.
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

// Define the props for the provider component
interface ThemeProviderProps {
  children: ReactNode;
}

// This is the provider component that will wrap your entire app.
export const ThemeProvider = ({ children }: ThemeProviderProps) => {
  // 1. State to hold the current theme. Initialize from localStorage or default to 'light'.
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const savedTheme = localStorage.getItem('theme');
    // You can also add logic here to check for user's system preference
    // const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    return (savedTheme as 'light' | 'dark') || 'light';
  });

  // 2. useEffect to apply the theme class to the <html> element whenever the theme changes.
  useEffect(() => {
    const root = window.document.documentElement;
    
    // Remove the old class and add the new one.
    root.classList.remove(theme === 'dark' ? 'light' : 'dark');
    root.classList.add(theme);

    // 3. Save the current theme preference to localStorage.
    localStorage.setItem('theme', theme);
  }, [theme]);

  // Function to toggle the theme
  const toggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === 'light' ? 'dark' : 'light'));
  };

  // The value object that will be available to all consuming components
  const value = { theme, toggleTheme };

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
};

// Custom hook for easy consumption of the context in your components
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
