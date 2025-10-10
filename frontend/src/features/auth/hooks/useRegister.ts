import { useState } from "react";
import { isValidEmail, isStrongPassword } from "../../../core/utils/validators";
import { register, toggle2FA } from "../services/authService";

// The hook no longer accepts any arguments.
export const useRegister = () => {
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
      setError("Password must be at least 8 chars long and include uppercase, lowercase, number, and special character.");
      return;
    }

    try {
      setLoading(true);
      await register({ email, password });

      // Assuming your toggle2FA service doesn't need any arguments
      // or can get the user context from the auth token set during register.
      if (enable2FA) {
        await toggle2FA();
      }

      // On success, we just change the step.
      setStep("verify");
    } catch (err: any) {
      setError(err.message || "Registration failed.");
    } finally {
      setLoading(false);
    }
  };

  // The onVerified property is no longer returned.
  return {
    email,
    setEmail,
    password,
    setPassword,
    enable2FA,
    setEnable2FA,
    step,
    error,
    loading,
    handleSubmit,
  };
};