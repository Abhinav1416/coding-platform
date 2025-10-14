import React, { useState, useEffect } from 'react';
import Confetti from 'react-confetti';
import { motion } from 'framer-motion';
import { FaTrophy, FaTimes, FaHandshake } from 'react-icons/fa';
import type { MatchResult, PlayerResult } from '../types/match';


const useWindowSize = () => {
    const [size, setSize] = useState([0, 0]);
    useEffect(() => {
        function updateSize() {
            setSize([window.innerWidth, window.innerHeight]);
        }
        window.addEventListener('resize', updateSize);
        updateSize();
        return () => window.removeEventListener('resize', updateSize);
    }, []);
    return size;
};


const PlayerResultDisplay = ({ }: { playerResult: PlayerResult }) => (
    <motion.div
        className="bg-gray-100 dark:bg-zinc-800 p-4 rounded-lg"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
    >
        <h4 className="font-bold text-xl text-gray-900 dark:text-white capitalize">{}</h4>
    </motion.div>
);

interface MatchResultOverlayProps {
  result: MatchResult;
  currentUserEmail?: string;
}

export const MatchResultOverlay: React.FC<MatchResultOverlayProps> = ({ result, currentUserEmail }) => {
  const [width, height] = useWindowSize();

  const currentUserUsername = currentUserEmail?.split('@')[0];
  let outcome: 'WIN' | 'LOSS' | 'DRAW' = 'DRAW';
  if (result.winnerUsername) {
      outcome = result.winnerUsername === currentUserUsername ? 'WIN' : 'LOSS';
  }


  const outcomeConfig = {
    WIN: { text: "Victory!", icon: <FaTrophy />, textColor: "text-green-600 dark:text-green-400", borderColor: "border-green-500", shadowColor: "shadow-[0_0_30px_5px_rgba(34,197,94,0.3)]" },
    LOSS: { text: "Defeat", icon: <FaTimes />, textColor: "text-red-600 dark:text-red-500", borderColor: "border-red-500", shadowColor: "shadow-[0_0_20px_5px_rgba(239,68,68,0.3)]" },
    DRAW: { text: "It's a Draw", icon: <FaHandshake />, textColor: "text-yellow-600 dark:text-yellow-400", borderColor: "border-yellow-500", shadowColor: "shadow-[0_0_20px_5px_rgba(234,179,8,0.3)]" }
  }[outcome];

  return (
    <motion.div
        className={`fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-4 ${outcome === 'LOSS' ? 'grayscale' : ''}`}
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.5 }}
    >
      {outcome === 'WIN' && <Confetti width={width} height={height} recycle={false} numberOfPieces={400} />}
      <motion.div
        className={`bg-white dark:bg-zinc-900 border-2 ${outcomeConfig.borderColor} ${outcomeConfig.shadowColor} rounded-xl p-8 pb-12 max-w-lg w-full text-center`}
        initial={{ y: "-100vh", opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ type: 'spring', stiffness: 100, damping: 15, delay: 0.2 }}
      >
        <motion.div
            className="flex justify-center items-center gap-4 mb-4"
            initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 300, damping: 10, delay: 0.8 }}
        >
          <div className={`text-5xl ${outcomeConfig.textColor}`}>{outcomeConfig.icon}</div>
          <h2 className={`text-5xl font-bold ${outcomeConfig.textColor}`}>{outcomeConfig.text}</h2>
        </motion.div>
        <motion.p className="text-gray-600 dark:text-gray-400 mb-6" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.2 }}>
            The match has ended. Here are the final results:
        </motion.p>
        <motion.div
            className="grid grid-cols-2 gap-4"
            initial="hidden" animate="visible"
            variants={{ hidden: {}, visible: { transition: { staggerChildren: 0.3, delayChildren: 1.5 } } }}
        >
            <motion.div variants={{ hidden: { opacity: 0, x: -50 }, visible: { opacity: 1, x: 0 } }}><PlayerResultDisplay playerResult={result.playerOne} /></motion.div>
            <motion.div variants={{ hidden: { opacity: 0, x: 50 }, visible: { opacity: 1, x: 0 } }}><PlayerResultDisplay playerResult={result.playerTwo} /></motion.div>
        </motion.div>
      </motion.div>
    </motion.div>
  );
};