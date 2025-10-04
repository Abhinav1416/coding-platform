import { useEffect } from 'react';
import AppRoutes from "./routes/AppRoutes";
import { ThemeProvider } from "./core/context/ThemeContext";
import { stompService } from './core/sockets/stompClient';

const App = () => {
  useEffect(() => {
    // Connect once when the app loads
    stompService.connect();

    // Disconnect when the app closes
    return () => {
      stompService.disconnect();
    };
  }, []); // Empty array ensures this runs only once

  return (
    <ThemeProvider>
      <AppRoutes />
    </ThemeProvider>
  );
};

export default App;