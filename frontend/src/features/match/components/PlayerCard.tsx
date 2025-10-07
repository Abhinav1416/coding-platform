import React from 'react';
import { Loader2 } from 'lucide-react';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake } from 'react-icons/fa';
import type { Player } from '../types/match';

// A small sub-component to keep the JSX clean
const StatItem = ({ icon, label, value }: { icon: React.ReactNode, label: string, value: number }) => (
    <div className="flex items-center gap-3">
        <div className="text-[#F97316] text-2xl flex-shrink-0 w-8 text-center">{icon}</div>
        <div>
            <p className="text-sm text-gray-400">{label}</p>
            <p className="text-lg font-bold text-white">{value}</p>
        </div>
    </div>
);

interface PlayerCardProps {
  player: Player | null;
}

export const PlayerCard: React.FC<PlayerCardProps> = ({ player }) => {
  if (!player) {
    return (
      <div className="bg-zinc-900/50 border-2 border-dashed border-zinc-700 rounded-xl p-6 flex flex-col items-center justify-center text-center h-full">
        <Loader2 className="animate-spin text-zinc-500 mb-4" size={32} />
        <p className="text-zinc-400 font-semibold">Waiting for opponent...</p>
      </div>
    );
  }

  return (
    <div className="bg-zinc-900 border border-white/10 rounded-xl p-6">
      <h3 className="text-2xl font-bold text-center text-white capitalize mb-6">{player.username}</h3>
      
      {/* Displaying the 4 new stats in a grid */}
      <div className="grid grid-cols-2 gap-x-4 gap-y-5">
          <StatItem icon={<FaGamepad />} label="Played" value={player.duelsPlayed} />
          <StatItem icon={<FaTrophy />} label="Won" value={player.duelsWon} />
          <StatItem icon={<FaTimesCircle />} label="Lost" value={player.duelsLost} />
          <StatItem icon={<FaHandshake />} label="Drawn" value={player.duelsDrawn} />
      </div>
    </div>
  );
};