import { useState } from "react";
import { Link } from "react-router-dom";
import { isValidEmail, isStrongPassword } from "../../../core/utils/validators";
import { register, toggle2FA } from "../services/authService";
import VerifyTokenForm from "./VerifyTokenForm";

interface Props {
  onVerified: () => void;
  theme: "light" | "dark";
}

const RegisterForm = ({ onVerified, theme }: Props) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [enable2FA, setEnable2FA] = useState(false);
  const [step, setStep] = useState<"register" | "verify">("register");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!isValidEmail(email)) {
      setError("Invalid email address.");
      return;
    }
    if (!isStrongPassword(password)) {
      setError(
        "Password must be at least 8 chars long and include uppercase, lowercase, number, and special character."
      );
      return;
    }

    try {
      setLoading(true);
      await register({ email, password });

      if (enable2FA) {
        await toggle2FA({ email });
      }

      setStep("verify");
    } catch (err: any) {
      setError(err.message || "Registration failed.");
    } finally {
      setLoading(false);
    }
  };

  const inputClasses =
    theme === "dark"
      ? "bg-slate-700 text-white border-slate-600 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500"
      : "bg-slate-50 text-slate-900 border-slate-300 placeholder-slate-400 focus:ring-indigo-500 focus:border-indigo-500";

  const textColor = theme === "dark" ? "text-slate-300" : "text-slate-600";

  if (step === "verify") {
    return (
      <VerifyTokenForm email={email} onVerified={onVerified} theme={theme} />
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && <div className="text-red-500 text-sm">{error}</div>}

      <div>
        <label
          htmlFor="email"
          className={`block text-sm font-medium mb-1 ${textColor}`}
        >
          Email
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading}
        />
      </div>

      <div>
        <label
          htmlFor="password"
          className={`block text-sm font-medium mb-1 ${textColor}`}
        >
          Password
        </label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          className={`w-full p-3 rounded-md border transition-colors duration-300 focus:outline-none focus:ring-2 ${inputClasses}`}
          required
          disabled={loading}
        />
      </div>

      <label className="flex items-center space-x-3">
        <input
          type="checkbox"
          checked={enable2FA}
          onChange={(e) => setEnable2FA(e.target.checked)}
          className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
          disabled={loading}
        />
        <span className={`text-sm ${textColor}`}>
          Enable Two-Factor Authentication
        </span>
      </label>

      <button
        type="submit"
        className="w-full bg-indigo-600 text-white font-bold p-3 rounded-md hover:bg-indigo-700 transition-colors duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
        disabled={loading}
      >
        {loading ? "Creating Account..." : "Create Account"}
      </button>

      <p className="text-center text-sm text-slate-500 dark:text-slate-400">
        Already have an account?{" "}
        <Link
          to="/login"
          className="font-semibold text-indigo-500 hover:text-indigo-400"
        >
          Sign in
        </Link>
      </p>
    </form>
  );
};

export default RegisterForm;
