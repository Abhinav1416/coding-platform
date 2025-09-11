import { useNavigate } from "react-router-dom";
import RegisterForm from "../components/RegisterForm";
import { useTheme } from "../../../core/context/ThemeContext";

const RegisterPage = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-900 text-white dark:bg-gray-900 dark:text-white light:bg-gray-100 light:text-black">
      {/* Top right theme toggle */}
      <button
        onClick={toggleTheme}
        className="absolute top-4 right-4 px-3 py-1 border rounded"
      >
        {theme === "dark" ? "Light Mode" : "Dark Mode"}
      </button>

      <h1 className="text-2xl mb-6">Create your account</h1>

      <RegisterForm onVerified={() => navigate("/home")} />
    </div>
  );
};

export default RegisterPage;
