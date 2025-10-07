import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useJoinMatch } from '../hooks/useJoinMatch';

const JoinMatchPage = () => {
  const [searchParams] = useSearchParams();
  const { joinMatchMutation, isLoading, error } = useJoinMatch();
  
  const [manualRoomCode, setManualRoomCode] = useState('');
  const codeFromUrl = searchParams.get('roomCode');

  // This effect handles the automatic join from a shared link
  useEffect(() => {
    if (codeFromUrl) {
      joinMatchMutation({ roomCode: codeFromUrl });
    }
  }, [codeFromUrl]); // Re-run if the URL param ever changes

  const handleManualSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (manualRoomCode.trim()) {
      joinMatchMutation({ roomCode: manualRoomCode.trim() });
    }
  };
  
  // If joining automatically from a link, show a full-page loader
  if (codeFromUrl) {
    return (
      <div className="min-h-screen bg-gray-900 text-gray-300 flex flex-col items-center justify-center p-4">
        <Loader2 className="h-12 w-12 animate-spin text-[#F97316] mb-4" />
        <h2 className="text-2xl font-bold text-white">Joining Match...</h2>
        {error && <p className="text-red-400 text-sm text-center mt-4">{error}</p>}
      </div>
    );
  }

  // Otherwise, show the manual join form
  return (
    <div className="min-h-screen bg-gray-900 text-gray-300 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-zinc-900 p-8 rounded-xl border border-white/10 shadow-lg">
        <h2 className="text-3xl font-bold text-center text-white mb-8">Join a Match</h2>
        <form onSubmit={handleManualSubmit} className="space-y-6">
          <div>
            <label htmlFor="roomCode" className="block text-sm font-medium text-gray-400 mb-2">
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
              className="w-full p-2 bg-zinc-800 border border-zinc-700 rounded-md outline-none transition-colors focus:border-[#F97316] focus:ring-1 focus:ring-[#F97316] tracking-widest text-center font-mono"
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
          {error && <p className="text-red-400 text-sm text-center mt-4">{error}</p>}
        </form>
      </div>
    </div>
  );
};

export default JoinMatchPage;