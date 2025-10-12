import { useEffect } from 'react';
import AppRoutes from "./routes/AppRoutes";
import { stompService } from './core/sockets/stompClient';

const App = () => {
  useEffect(() => {
    stompService.connect();
    return () => {
      stompService.disconnect();
    };
  }, []);

  return (
    <div className="bg-gray-50 dark:bg-zinc-950 min-h-screen">
      <AppRoutes />
    </div>
  );
};

export default App;
