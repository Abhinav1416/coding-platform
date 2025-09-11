import AppRoutes from "./routes/AppRoutes";
import { ThemeProvider } from "./core/context/ThemeContext";

const App = () => {
  return (
    <ThemeProvider>
      <AppRoutes />
    </ThemeProvider>
  );
};

export default App;
