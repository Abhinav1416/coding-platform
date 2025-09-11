import { useState } from "react";
import { isValidEmail, isStrongPassword } from "../../../core/utils/validators";
import { register, toggle2FA } from "../services/authService";
import VerifyTokenForm from "./VerifyTokenForm";

interface Props {
  onVerified: () => void;
}

const RegisterForm = ({ onVerified }: Props) => {
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
        "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character."
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

  if (step === "verify") {
    return <VerifyTokenForm email={email} onVerified={onVerified} />;
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4 w-full max-w-md">
      {error && <div className="text-red-500">{error}</div>}

      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email"
        className="w-full p-3 rounded bg-gray-800 text-white"
        required
        disabled={loading}
      />

      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Password"
        className="w-full p-3 rounded bg-gray-800 text-white"
        required
        disabled={loading}
      />

      <label className="flex items-center space-x-2 text-white">
        <input
          type="checkbox"
          checked={enable2FA}
          onChange={(e) => setEnable2FA(e.target.checked)}
          disabled={loading}
        />
        <span>Enable Two-Factor Authentication</span>
      </label>

      <button
        type="submit"
        className="w-full bg-blue-600 text-white p-3 rounded hover:bg-blue-700 disabled:opacity-50"
        disabled={loading}
      >
        {loading ? "Registering..." : "Create Account"}
      </button>
    </form>
  );
};

export default RegisterForm;
