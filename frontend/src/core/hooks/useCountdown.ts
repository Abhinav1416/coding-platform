import { useState, useEffect } from 'react';

const getReturnValues = (countDown: number) => {
  const minutes = Math.floor((countDown % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((countDown % (1000 * 60)) / 1000);
  return { minutes, seconds, isFinished: countDown <= 0 };
};

export const useCountdown = (targetDate: string) => {
  const countDownDate = new Date(targetDate).getTime();

  const [countDown, setCountDown] = useState(
    countDownDate - new Date().getTime()
  );

  useEffect(() => {

    if (isNaN(countDownDate)) {
      setCountDown(0);
      return;
    }

    const interval = setInterval(() => {
      const remaining = countDownDate - new Date().getTime();
      setCountDown(remaining > 0 ? remaining : 0);
    }, 1000);


    return () => clearInterval(interval);
  }, [countDownDate]);

  return getReturnValues(countDown);
};