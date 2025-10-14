import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useJoinMatch } from '../hooks/useJoinMatch';
import MainLayout from '../../../components/layout/MainLayout';



const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
};

const JoinMatchPage = () => {
  const [searchParams] = useSearchParams();
  const { joinMatchMutation, isLoading, error } = useJoinMatch();

  const [manualRoomCode, setManualRoomCode] = useState('');
  const codeFromUrl = searchParams.get('roomCode');


  useEffect(() => {
    if (codeFromUrl) {
      joinMatchMutation({ roomCode: codeFromUrl });
    }
  }, [codeFromUrl, joinMatchMutation]);


  if (codeFromUrl) {
    return (
      <MainLayout>
        <div className="flex flex-col items-center justify-center pt-24">
          <Loader2 className="h-12 w-12 animate-spin text-[#F97316] mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Joining Match...</h2>
          {error && <p className="text-red-500 text-sm text-center mt-4">{getErrorMessage(error)}</p>}
        </div>
      </MainLayout>
    );
  }

  const handleManualSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (manualRoomCode.trim()) {
      joinMatchMutation({ roomCode: manualRoomCode.trim() });
    }
  };


  return (
    <MainLayout>
      <div className="flex items-center justify-center">
        <div className="w-full max-w-md bg-white dark:bg-zinc-900 p-8 rounded-xl border border-gray-200 dark:border-white/10 shadow-lg">
          <h2 className="text-3xl font-bold text-center text-gray-900 dark:text-white mb-8">Join a Match</h2>
          <form onSubmit={handleManualSubmit} className="space-y-6">
            <div>
              <label htmlFor="roomCode" className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">
                Enter Room Code
              </label>
              <input
                type="text"
                id="roomCode"
                name="roomCode"
                value={manualRoomCode}
                onChange={(e) => setManualRoomCode(e.target.value.toUpperCase())}
                placeholder="e.g., ABCDEF"
                required
                className="w-full p-2 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-gray-200 rounded-md outline-none transition-colors focus:border-[#F97316] focus:ring-1 focus:ring-[#F97316] tracking-widest text-center font-mono"
              />
            </div>
            <div>
              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
              >
                {isLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Join Match'}
              </button>
            </div>
            {error && <p className="text-red-500 text-sm text-center mt-4">{getErrorMessage(error)}</p>}
          </form>
        </div>
      </div>
    </MainLayout>
  );
};

export default JoinMatchPage;