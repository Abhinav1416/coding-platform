import React from 'react';
import { useParams, Link } from 'react-router-dom';

const MatchResultPage = () => {
  const { matchId } = useParams<{ matchId: string }>();

  return (
    <div className="min-h-screen bg-gray-900 text-gray-300 flex flex-col items-center justify-center p-4 text-center">
      <h1 className="text-4xl font-bold text-white mb-4">
        Match Results
      </h1>
      <p className="text-lg text-gray-400 mb-8">
        This is the match results page. The full summary will be built here later.
      </p>
      <p className="font-mono text-sm text-zinc-500 bg-zinc-800 px-3 py-1 rounded-md">
        Match ID: {matchId}
      </p>
      <Link to="/home" className="mt-8 bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-6 rounded">
        Return to Home
      </Link>
    </div>
  );
};

export default MatchResultPage; 