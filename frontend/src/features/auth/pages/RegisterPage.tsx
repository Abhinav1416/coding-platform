import { useNavigate } from "react-router-dom";
import RegisterForm from "../components/RegisterForm";
import ThemeToggle from "../components/ThemeToggle";
import { useTheme } from "../../../core/context/ThemeContext";

const RegisterPage = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  return (
    <div
      className={`min-h-screen flex flex-col items-center justify-center p-4 transition-colors duration-300 ${
        theme === "dark" ? "bg-slate-900" : "bg-slate-100"
      }`}
    >
      <ThemeToggle theme={theme} toggleTheme={toggleTheme} />

      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className={`text-3xl font-bold ${theme === 'dark' ? 'text-white' : 'text-slate-800'}`}>
            Create Your Account
          </h1>
          <p className={`mt-2 ${theme === 'dark' ? 'text-slate-400' : 'text-slate-600'}`}>
            Join us and start your journey!
          </p>
        </div>

        <div
          className={`p-8 rounded-xl shadow-lg transition-colors duration-300 w-full ${
            theme === "dark" ? "bg-slate-800" : "bg-white"
          }`}
        >
          <RegisterForm onVerified={() => navigate("/home")} theme={theme} />
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;