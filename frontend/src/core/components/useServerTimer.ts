import { useState, useEffect } from 'react';

/**
 * A self-correcting timer that relies on a server-authoritative start time.
 * This hook is resilient to browser tab throttling and client/server clock skew.
 * @param startTime The UTC timestamp (in ms) when the timer started, from the server.
 * @param durationInSeconds The total duration of the timer, from the server.
 */
export const useServerTimer = (startTime: number | null, durationInSeconds: number | null) => {
  // This state's only purpose is to force a re-render on an interval.
  const [, setTick] = useState(0);

  useEffect(() => {
    // Do not start an interval if we don't have the necessary data.
    if (startTime === null || durationInSeconds === null) {
      return;
    }

    // Set up the interval to trigger a re-render every 500ms.
    const timerInterval = setInterval(() => {
      // By updating state, we ensure the component using this hook re-renders.
      setTick(prevTick => prevTick + 1);
    }, 500);

    // Cleanup the interval when the component unmounts or props change.
    return () => clearInterval(timerInterval);
  }, [startTime, durationInSeconds]);

  // If we don't have data, return a default state.
  if (startTime === null || durationInSeconds === null) {
    return { minutes: 0, seconds: 0, totalSeconds: 0, isFinished: false };
  }

  // --- Calculation is now done on every render ---
  // This ensures the timer appears instantly when props are available.
  const totalDurationMs = durationInSeconds * 1000;
  const elapsedTime = Date.now() - startTime;
  const remainingTime = totalDurationMs - elapsedTime;

  // Cap the time to prevent display issues from clock skew.
  const cappedRemainingTime = Math.min(remainingTime, totalDurationMs);
  const finalRemainingTime = cappedRemainingTime > 0 ? cappedRemainingTime : 0;

  const totalSeconds = Math.floor(finalRemainingTime / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  const isFinished = finalRemainingTime <= 0;

  return {
    minutes,
    seconds,
    totalSeconds,
    isFinished,
  };
};