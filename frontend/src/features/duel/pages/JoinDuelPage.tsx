import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, Info } from 'lucide-react';
import { toast } from 'react-hot-toast';
import MainLayout from '../../../components/layout/MainLayout';
import { duelService } from '../api/duelService';
import { useAuth } from '../../../core/hooks/useAuth';

const JoinDuelPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [roomCode, setRoomCode] = useState('');
  const [handle, setHandle] = useState(''); 
  const [isLoading, setIsLoading] = useState(false);

  const handleManualSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      toast.error("You must be logged in to join a duel.");
      navigate('/login');
      return;
    }

    if (!roomCode.trim() || !handle.trim()) return toast.error("Please fill all fields");

    setIsLoading(true);
    try {
      const response = await duelService.joinDuel(roomCode.trim(), handle.trim());
      toast.success("Joined Duel!");
      navigate(`/duel/lobby/${response.duelId}`);
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Failed to join duel';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <MainLayout>
      <div className="flex items-center justify-center pt-20 px-4">
        <div className="w-full max-w-md bg-white dark:bg-zinc-900 p-8 rounded-xl border border-gray-200 dark:border-zinc-700 shadow-lg">
          <h2 className="text-3xl font-bold text-center text-gray-900 dark:text-white mb-8">Join a Duel</h2>
          <form onSubmit={handleManualSubmit} className="space-y-6">
            
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Room Code</label>
              <input
                type="number"
                value={roomCode}
                onChange={(e) => setRoomCode(e.target.value)}
                placeholder="e.g. 123456"
                required
                className="w-full p-2 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-white rounded-md outline-none focus:border-[#F97316] text-center font-mono tracking-widest text-lg transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Your CF Handle</label>
              <input
                type="text"
                value={handle}
                onChange={(e) => setHandle(e.target.value)}
                placeholder="e.g. tourist"
                required
                className="w-full p-2 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-white rounded-md outline-none focus:border-[#F97316] transition-colors"
              />
              
              <div className="flex gap-2 mt-3 text-orange-600 dark:text-orange-400 bg-orange-50 dark:bg-orange-900/10 p-3 rounded-md text-xs border border-orange-100 dark:border-orange-900/20">
                  <Info size={16} className="shrink-0" />
                  <p>
                    <strong>Double check this!</strong> We use this handle to fetch your submissions from Codeforces. If it's wrong, your score will stay 0.
                  </p>
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
            >
              {isLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Join Match'}
            </button>
          </form>
        </div>
      </div>
    </MainLayout>
  );
};

export default JoinDuelPage;