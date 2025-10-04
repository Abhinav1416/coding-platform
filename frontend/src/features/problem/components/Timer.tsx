import React from 'react';

interface TimerProps {
    time: number;
    isActive: boolean;
    onStart: () => void;
    onPause: () => void;
    onReset: () => void;
}

const formatTime = (totalSeconds: number): string => {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    const pad = (num: number) => num.toString().padStart(2, '0');

    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
};

const Timer: React.FC<TimerProps> = ({ time, isActive, onStart, onPause, onReset }) => {
    return (
        <div className="flex items-center gap-4">
            <span className="font-mono text-lg text-gray-800 dark:text-gray-200 bg-gray-200 dark:bg-gray-700 px-3 py-1 rounded-md">
                {formatTime(time)}
            </span>
            <div className="flex items-center gap-2">
                {isActive ? (
                    <button onClick={onPause} aria-label="Pause Timer" className="flex items-center justify-center w-8 h-8 bg-yellow-500 text-white rounded-full hover:bg-yellow-600 transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                          <path fillRule="evenodd" d="M13.5 4.5A1.5 1.5 0 0 0 12 6v8a1.5 1.5 0 0 0 3 0V6a1.5 1.5 0 0 0-1.5-1.5Zm-8 0A1.5 1.5 0 0 0 4 6v8a1.5 1.5 0 0 0 3 0V6A1.5 1.5 0 0 0 5.5 4.5Z" clipRule="evenodd" />
                        </svg>
                    </button>
                ) : (
                    <button onClick={onStart} aria-label="Start or Resume Timer" className="flex items-center justify-center w-8 h-8 bg-green-600 text-white rounded-full hover:bg-green-700 transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                          <path d="M6.3 2.841A1.5 1.5 0 0 0 4 4.11V15.89a1.5 1.5 0 0 0 2.3 1.269l9.344-5.89a1.5 1.5 0 0 0 0-2.538L6.3 2.841Z" />
                        </svg>
                    </button>
                )}
                <button onClick={onReset} aria-label="Reset Timer" className="flex items-center justify-center w-8 h-8 bg-red-600 text-white rounded-full hover:bg-red-700 disabled:bg-gray-500 disabled:cursor-not-allowed transition-colors" disabled={time === 0 && !isActive}>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                      <path fillRule="evenodd" d="M15.312 11.342a1.25 1.25 0 0 1 .176-1.77l.176-.177a1.25 1.25 0 1 1 1.768 1.768l-.176.177a1.25 1.25 0 0 1-1.768 0ZM12.03 15.03a1.25 1.25 0 0 1-1.768 0l-.176-.177a1.25 1.25 0 1 1 1.768-1.768l.176.177a1.25 1.25 0 0 1 0 1.768ZM10 4.875A5.125 5.125 0 0 0 4.875 10H2.5a.75.75 0 0 0 0 1.5h2.375A5.125 5.125 0 0 0 10 16.625a5.13 5.13 0 0 0 5.125-5.125h2.375a.75.75 0 0 0 0-1.5H15.125A5.125 5.125 0 0 0 10 4.875Z" clipRule="evenodd" />
                    </svg>
                </button>
            </div>
        </div>
    );
};

export default Timer;