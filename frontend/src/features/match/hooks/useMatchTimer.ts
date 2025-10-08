import { useState, useEffect } from 'react';

// The type for matchState now includes 'AWAITING_RESULT' and removes 'WAITING_FOR_START'
type ArenaMatchState = 'LOADING' | 'IN_PROGRESS' | 'AWAITING_RESULT' | 'COMPLETED';

export const useMatchTimer = (
    initialDuration: number, 
    matchState: ArenaMatchState
) => {
    const [timeLeft, setTimeLeft] = useState(initialDuration);

    useEffect(() => {
        // Set the initial time when the component loads or when the duration is known
        if (initialDuration > 0) {
           setTimeLeft(initialDuration);
        }
    }, [initialDuration]);

    useEffect(() => {
        // The timer should only be ticking when the match is IN_PROGRESS.
        // It will stop if the state changes to COMPLETED or AWAITING_RESULT.
        if (matchState !== 'IN_PROGRESS' || timeLeft <= 0) {
            return;
        }

        const timerInterval = setInterval(() => {
            setTimeLeft(prevTime => prevTime - 1);
        }, 1000);

        // Cleanup function to clear the interval
        return () => clearInterval(timerInterval);
    }, [matchState, timeLeft]);

    return { timeLeft };
};