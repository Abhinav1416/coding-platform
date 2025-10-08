import React from 'react';
import type { MatchResult } from '../types/match';

interface MatchResultOverlayProps {
    result: MatchResult;
    onClose: () => void; // For future use, e.g., navigating away
}

const MatchResultOverlay: React.FC<MatchResultOverlayProps> = ({ result, onClose }) => (
    <div className="fixed inset-0 bg-black bg-opacity-75 flex justify-center items-center z-50">
        <div className="bg-gray-800 rounded-lg p-8 text-center shadow-xl border border-gray-700">
            <h2 className="text-3xl font-bold text-white mb-4">Match Over!</h2>
            {result.winnerUsername ? (
                <p className="text-xl text-green-400">Winner: {result.winnerUsername}</p>
            ) : (
                <p className="text-xl text-yellow-400">It's a Draw!</p>
            )}
            <p className="text-gray-400 mt-2">Reason: {result.reason}</p>
            <button onClick={onClose} className="mt-6 bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-6 rounded">
                Go to Dashboard
            </button>
        </div>
    </div>
);

export default MatchResultOverlay;