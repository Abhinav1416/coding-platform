import { useEffect, useState } from 'react';
// Adjust the import paths below to match your project structure
import { getCurrentUser } from '../features/auth/services/authService';
import type { User } from '../features/auth/types/auth';

const HomePage = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        setIsLoading(true);
        const currentUser = await getCurrentUser();
        setUser(currentUser);
      } catch (err: any) {
        console.error("Failed to fetch user:", err);
        setError("Could not load user data.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, []);

  if (isLoading) {
    return <div className="p-6 text-xl">Loading user data...</div>;
  }

  if (error) {
    return <div className="p-6 text-xl text-red-500">{error}</div>;
  }

  return (
    <div className="p-6 text-xl">
      {/* --- THIS LINE IS NOW CORRECT --- */}
      Welcome, {user?.email || 'User'}!
    </div>
  );
};

export default HomePage;