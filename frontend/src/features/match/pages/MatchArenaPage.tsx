import React from 'react';
import { useParams } from 'react-router-dom';

const MatchArenaPage = () => {
  // Get the matchId from the URL to confirm it's being passed correctly
  const { matchId } = useParams<{ matchId: string }>();

  return (
    <div className="min-h-screen bg-gray-900 text-gray-300 flex flex-col items-center justify-center p-4">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-white mb-4">
          Match Arena
        </h1>
        <p className="text-lg text-gray-400 mb-2">
          You have successfully entered the arena!
        </p>
        <p className="font-mono text-sm text-zinc-500 bg-zinc-800 px-3 py-1 rounded-md">
          Match ID: {matchId}
        </p>
        <p className="mt-8 text-gray-500">
          (The full arena UI will be built here later)
        </p>
      </div>
    </div>
  );
};

export default MatchArenaPage;