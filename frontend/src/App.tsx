import { useEffect } from 'react';
import AppRoutes from "./routes/AppRoutes";
import { stompService } from './core/sockets/stompClient';

// We removed the unnecessary ThemeProvider wrapper
const App = () => {
  useEffect(() => {
    stompService.connect();
    return () => {
      stompService.disconnect();
    };
  }, []);

  return (
    <AppRoutes />
  );
};

export default App;