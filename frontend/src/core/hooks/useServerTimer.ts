import { useState, useEffect } from 'react';

export const useServerTimer = (startTime?: number, durationMinutes?: number) => {
  const [timeLeft, setTimeLeft] = useState("00:00");
  const [isEnded, setIsEnded] = useState(false);

  useEffect(() => {
    if (!startTime || !durationMinutes) return;

    const endTime = startTime * 1000 + (durationMinutes * 60 * 1000);

    const tick = () => {
      const now = Date.now();
      const diff = endTime - now;

      if (diff <= 0) {
        setTimeLeft("00:00");
        setIsEnded(true);
        return;
      }

      const m = Math.floor(diff / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      setTimeLeft(`${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`);
    };

    tick();
    const interval = setInterval(tick, 1000);

    return () => clearInterval(interval);
  }, [startTime, durationMinutes]);

  return { timeLeft, isEnded };
};