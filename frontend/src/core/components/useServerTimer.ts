import { useState, useEffect } from 'react';


export const useServerTimer = (startTime: number | null, durationInSeconds: number | null) => {

  const [, setTick] = useState(0);

  useEffect(() => {
    if (startTime === null || durationInSeconds === null) {
      return;
    }
    const timerInterval = setInterval(() => {     
      setTick(prevTick => prevTick + 1);
    }, 500);

    return () => clearInterval(timerInterval);
  }, [startTime, durationInSeconds]);

  if (startTime === null || durationInSeconds === null) {
    return { minutes: 0, seconds: 0, totalSeconds: 0, isFinished: false };
  }

  const totalDurationMs = durationInSeconds * 1000;
  const elapsedTime = Date.now() - startTime;
  const remainingTime = totalDurationMs - elapsedTime;

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