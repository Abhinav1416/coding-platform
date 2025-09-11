import { useState } from "react";
import { verifyEmail } from "../services/authService";

interface Props {
  email: string;
  onVerified: () => void;
}

const VerifyTokenForm = ({ email, onVerified }: Props) => {
  const [token, setToken] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    try {
      setLoading(true);
      await verifyEmail({ email, token });
      onVerified();
    } catch (err: any) {
      setError(err.message || "Invalid verification token.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleVerify} className="space-y-4 w-full max-w-md">
      {error && <div className="text-red-500">{error}</div>}

      <input
        type="text"
        value={token}
        onChange={(e) => setToken(e.target.value)}
        placeholder="Enter verification token"
        className="w-full p-3 rounded bg-gray-800 text-white"
        required
        disabled={loading}
      />

      <button
        type="submit"
        className="w-full bg-green-600 text-white p-3 rounded hover:bg-green-700 disabled:opacity-50"
        disabled={loading}
      >
        {loading ? "Verifying..." : "Verify Email"}
      </button>
    </form>
  );
};

export default VerifyTokenForm;
