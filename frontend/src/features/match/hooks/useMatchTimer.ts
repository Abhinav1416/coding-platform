import { useState, useEffect } from 'react';

type ArenaMatchState = 'LOADING' | 'IN_PROGRESS' | 'AWAITING_RESULT' | 'COMPLETED';

export const useMatchTimer = (
    initialDuration: number, 
    matchState: ArenaMatchState
) => {
    const [timeLeft, setTimeLeft] = useState(initialDuration);

    useEffect(() => {

        if (initialDuration > 0) {
           setTimeLeft(initialDuration);
        }
    }, [initialDuration]);

    useEffect(() => {
        if (matchState !== 'IN_PROGRESS' || timeLeft <= 0) {
            return;
        }

        const timerInterval = setInterval(() => {
            setTimeLeft(prevTime => prevTime - 1);
        }, 1000);


        return () => clearInterval(timerInterval);
    }, [matchState, timeLeft]);

    return { timeLeft };
};