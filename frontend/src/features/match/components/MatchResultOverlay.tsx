import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Confetti from 'react-confetti';
import { motion } from 'framer-motion';
import { FaTrophy, FaTimes, FaHandshake } from 'react-icons/fa';
import type { MatchResult, PlayerResult } from '../types/match';

// A small helper hook to get window dimensions for the confetti
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

// Sub-component to display each player's final score
const PlayerResultDisplay = ({ playerResult }: { playerResult: PlayerResult }) => (
    <motion.div 
        className="bg-zinc-800 p-4 rounded-lg"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
    >
        <h4 className="font-bold text-xl text-white capitalize">{playerResult.username}</h4>
        <p className="text-2xl font-bold text-[#F97316]">{playerResult.score} <span className="text-base font-normal text-gray-400">pts</span></p>
        <p className="text-sm text-gray-500">Penalties: {playerResult.penalties}</p>
    </motion.div>
);

interface MatchResultOverlayProps {
  result: MatchResult;
  currentUserEmail?: string; // ✅ CHANGED: We now accept the user's email
}

export const MatchResultOverlay: React.FC<MatchResultOverlayProps> = ({ result, currentUserEmail }) => {
  const navigate = useNavigate();
  const [width, height] = useWindowSize();

  // ✅ THIS IS THE CORE LOGIC THAT FIXES THE BUG (using usernames)
  const currentUserUsername = currentUserEmail?.split('@')[0];
  let outcome: 'WIN' | 'LOSS' | 'DRAW' = 'DRAW';
  if (result.winnerUsername) {
      outcome = result.winnerUsername === currentUserUsername ? 'WIN' : 'LOSS';
  }

  // Configure text, icons, and colors based on the outcome
  const outcomeConfig = {
    WIN: { text: "Victory!", icon: <FaTrophy />, textColor: "text-green-400", borderColor: "border-green-500", shadowColor: "shadow-[0_0_30px_5px_rgba(34,197,94,0.5)]" },
    LOSS: { text: "Defeat", icon: <FaTimes />, textColor: "text-red-500", borderColor: "border-red-500", shadowColor: "shadow-[0_0_20px_5px_rgba(239,68,68,0.4)]" },
    DRAW: { text: "It's a Draw", icon: <FaHandshake />, textColor: "text-yellow-400", borderColor: "border-yellow-500", shadowColor: "shadow-[0_0_20px_5px_rgba(234,179,8,0.4)]" }
  }[outcome];

  return (
    <motion.div 
        className={`fixed inset-0 bg-black/80 flex items-center justify-center z-50 ${outcome === 'LOSS' ? 'grayscale' : ''}`}
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.5 }}
    >
      {outcome === 'WIN' && <Confetti width={width} height={height} recycle={false} numberOfPieces={400} />}
      <motion.div 
        className={`bg-zinc-900 border-2 ${outcomeConfig.borderColor} ${outcomeConfig.shadowColor} rounded-xl p-8 max-w-lg w-full text-center`}
        initial={{ y: "-100vh", opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ type: 'spring', stiffness: 100, damping: 15, delay: 0.2 }}
      >
        <motion.div 
            className="flex justify-center items-center gap-4 mb-4"
            initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 300, damping: 10, delay: 0.8 }}
        >
          <div className={`text-5xl ${outcomeConfig.textColor}`}>{outcomeConfig.icon}</div>
          <h2 className={`text-5xl font-bold ${outcomeConfig.textColor}`}>{outcomeConfig.text}</h2>
        </motion.div>
        <motion.p className="text-gray-400 mb-6" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.2 }}>
            The match has ended. Here are the final results:
        </motion.p>
        <motion.div 
            className="grid grid-cols-2 gap-4 mb-8"
            initial="hidden" animate="visible"
            variants={{ hidden: {}, visible: { transition: { staggerChildren: 0.3, delayChildren: 1.5 } } }}
        >
            <motion.div variants={{ hidden: { opacity: 0, x: -50 }, visible: { opacity: 1, x: 0 } }}><PlayerResultDisplay playerResult={result.playerOne} /></motion.div>
            <motion.div variants={{ hidden: { opacity: 0, x: 50 }, visible: { opacity: 1, x: 0 } }}><PlayerResultDisplay playerResult={result.playerTwo} /></motion.div>
        </motion.div>
        <motion.button 
          onClick={() => navigate('/home')}
          className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-6 rounded-md"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 2.0 }}
          whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}
        >
          Return to Home
        </motion.button>
      </motion.div>
    </motion.div>
  );
};