import React from 'react';

interface MatchHeaderProps {
    timeLeft: number;
    playerOneUsername: string;
    playerTwoUsername: string;
    status: string;
}

const formatTime = (seconds: number) => {
    if (seconds < 0) seconds = 0;
    const mins = Math.floor(seconds / 60).toString().padStart(2, '0');
    const secs = (seconds % 60).toString().padStart(2, '0');
    return `${mins}:${secs}`;
};

const MatchHeader: React.FC<MatchHeaderProps> = ({ timeLeft, playerOneUsername, playerTwoUsername, status }) => {
    return (
        <div className="bg-gray-800 text-white p-3 flex justify-between items-center sticky top-0 z-10 border-b border-gray-700">
            <div className="font-mono text-lg w-1/3 text-left">{playerOneUsername}</div>
            <div className="text-center w-1/3">
                <div className="text-3xl font-bold font-mono">{formatTime(timeLeft)}</div>
                <div className={`text-sm font-semibold uppercase tracking-wider ${status === 'IN_PROGRESS' ? 'text-green-400' : 'text-yellow-400'}`}>
                    {status.replace('_', ' ')}
                </div>
            </div>
            <div className="font-mono text-lg w-1/3 text-right">{playerTwoUsername}</div>
        </div>
    );
};

export default MatchHeader;