import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from './core/context/ThemeContext';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { AuthProvider } from './core/context/AuthContext';

// --- Import BrowserRouter HERE ---
import { BrowserRouter } from 'react-router-dom';

const queryClient = new QueryClient();
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    {/* --- WRAP EVERYTHING IN BROWSERROUTER --- */}
    <BrowserRouter>
      <GoogleOAuthProvider clientId={googleClientId}>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            <AuthProvider>
              <App />
            </AuthProvider>
          </ThemeProvider>
        </QueryClientProvider>
      </GoogleOAuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);