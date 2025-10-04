import React from 'react';
import { useNavigate } from 'react-router-dom';
import { FaCode, FaTrophy, FaChartBar, FaAngleRight } from 'react-icons/fa';


type UserStats = {
  problemsSolved: number;
  duelsWon: number;
  currentRank: string;
};

type DuelResult = 'Win' | 'Loss';

type Duel = {
  id: number;
  problem: string;
  opponent: string;
  result: DuelResult;
  time: string;
};



const userStats: UserStats = {
  problemsSolved: 42,
  duelsWon: 15,
  currentRank: 'Gold II',
};

const mockRecentDuels: Duel[] = [
  { id: 1, problem: 'Two Sum', opponent: 'user_alpha', result: 'Win', time: '2h ago' },
  { id: 2, problem: 'Reverse Linked List', opponent: 'user_beta', result: 'Loss', time: '1d ago' },
  { id: 3, problem: 'FizzBuzz', opponent: 'user_gamma', result: 'Win', time: '2d ago' },
];


const Home: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="space-y-12">
      <section className="text-center bg-zinc-900/50 p-8 rounded-lg border border-white/10">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-3">
          Welcome Back, Coder!
        </h1>
        <p className="text-lg text-gray-400 mb-8 max-w-2xl mx-auto">
          Ready to sharpen your skills? Start a new duel or browse our collection of problems.
        </p>
        <div className="flex justify-center gap-4">
          <button 
            onClick={() => alert('Navigate to Duel matchmaking page!')}
            className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-6 rounded-md transition-transform transform hover:scale-105"
          >
            Start New Duel
          </button>
          <button 
            onClick={() => navigate('/problems')}
            className="bg-transparent hover:bg-zinc-800 text-gray-200 font-semibold py-3 px-6 border border-zinc-700 rounded-md transition-colors"
          >
            Browse Problems
          </button>
        </div>
      </section>
      
      <section>
        <h2 className="text-2xl font-semibold mb-6 text-white">Your Stats</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      
          <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4">
            <FaCode className="text-[#F97316] text-3xl"/>
            <div>
              <p className="text-sm text-gray-400">Problems Solved</p>
              <p className="text-2xl font-bold text-white">{userStats.problemsSolved}</p>
            </div>
          </div>
      
          <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4">
            <FaTrophy className="text-[#F97316] text-3xl"/>
            <div>
              <p className="text-sm text-gray-400">Duels Won</p>
              <p className="text-2xl font-bold text-white">{userStats.duelsWon}</p>
            </div>
          </div>

          <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4">
            <FaChartBar className="text-[#F97316] text-3xl"/>
            <div>
              <p className="text-sm text-gray-400">Current Rank</p>
              <p className="text-2xl font-bold text-white">{userStats.currentRank}</p>
            </div>
          </div>
        </div>
      </section>


      <section>
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold text-white">Recent Duels</h2>
          <button onClick={() => navigate('/matches')} className="text-[#F97316] hover:text-[#EA580C] font-semibold flex items-center gap-1">
            View All <FaAngleRight />
          </button>
        </div>
        <div className="bg-zinc-900 rounded-lg border border-white/10">
          <ul className="divide-y divide-zinc-800">
            {mockRecentDuels.map((duel: Duel) => (
              <li key={duel.id} className="p-4 flex justify-between items-center hover:bg-zinc-800/50 transition-colors">
                <div>
                  <p className="font-semibold text-white">{duel.problem}</p>
                  <p className="text-sm text-gray-400">vs {duel.opponent}</p>
                </div>
                <div className="text-right">
                   <span className={`px-3 py-1 text-xs font-bold rounded-full ${
                      duel.result === 'Win' ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                    }`}>
                     {duel.result}
                   </span>
                  <p className="text-sm text-gray-500 mt-1">{duel.time}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </div>
  );
};

export default Home;