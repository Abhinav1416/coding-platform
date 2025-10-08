import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake, FaAngleRight } from 'react-icons/fa';
import { Loader2 } from 'lucide-react';
import { getCurrentUserStats, getMatchHistory } from '../features/match/services/matchService';
import { getProblemCount } from '../features/problem/services/problemService';
import type { UserStats, PastMatch } from '../features/match/types/match';
import { formatDistanceToNow } from 'date-fns';
import { useAuth } from '../core/hooks/useAuth';



const Home: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [stats, setStats] = useState<UserStats | null>(null);
  const [recentMatches, setRecentMatches] = useState<PastMatch[]>([]);
  const [problemCount, setProblemCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
        setIsLoading(false);
        return;
    }

    const fetchData = async () => {
      try {
        const [statsData, historyData, problemCountData] = await Promise.all([
          getCurrentUserStats(),
          getMatchHistory({ page: 0, size: 3 }),
          getProblemCount()
        ]);
        setStats(statsData);
        setRecentMatches(historyData.content);
        setProblemCount(problemCountData.totalCount);
      } catch (err) {
        setError("Failed to load dashboard data. Please try again later.");
        console.error(err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [user]);

  if (isLoading) {
    return <div className="flex justify-center items-center pt-24"><Loader2 className="animate-spin text-[#F97316]" size={48} /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 pt-24">{error}</div>;
  }

  return (
    <div className="space-y-12">
      <section className="text-center bg-zinc-900/50 p-8 rounded-lg border border-white/10">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-3">
          Welcome Back, <span className="capitalize">{user?.email.split('@')[0]}</span>!
        </h1>
        
        {/* ✅ "Sexy" placement for problem count */}
        <p className="text-lg text-gray-400 mb-8 max-w-2xl mx-auto">
          Ready to sharpen your skills? Test yourself against our library of <strong className="text-white font-bold">{problemCount}</strong> challenges or start a new match.
        </p>

        <div className="flex justify-center gap-4">
          <button 
            onClick={() => navigate('/match/create')}
            className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-6 rounded-md transition-transform transform hover:scale-105"
          >
            Start New Match
          </button>
        </div>
      </section>
      
      {stats && (
        <section>
          {/* ✅ Renamed section header */}
          <h2 className="text-2xl font-semibold mb-6 text-white">Your Stats</h2>
          
          {/* ✅ Grid updated for 4 items, "Total Problems" card removed */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4"><FaGamepad className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Played</p><p className="text-2xl font-bold text-white">{stats.duelsPlayed}</p></div></div>
              <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4"><FaTrophy className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Won</p><p className="text-2xl font-bold text-white">{stats.duelsWon}</p></div></div>
              <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4"><FaTimesCircle className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Lost</p><p className="text-2xl font-bold text-white">{stats.duelsLost}</p></div></div>
              <div className="bg-zinc-900 p-6 rounded-lg border border-white/10 flex items-center gap-4"><FaHandshake className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Drawn</p><p className="text-2xl font-bold text-white">{stats.duelsDrawn}</p></div></div>
          </div>
        </section>
      )}

      <section>
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold text-white">Recent Matches</h2>
          <button onClick={() => navigate('/matches/history')} className="text-[#F97316] hover:text-[#EA580C] font-semibold flex items-center gap-1">
            View All <FaAngleRight />
          </button>
        </div>
        <div className="bg-zinc-900 rounded-lg border border-white/10">
          {recentMatches.length > 0 ? (
            <ul className="divide-y divide-zinc-800">
              {recentMatches.map((match) => (
                <li key={match.matchId} className="p-4 flex justify-between items-center hover:bg-zinc-800/50 transition-colors">
                  <div>
                    <p className="font-semibold text-white">{match.problemTitle}</p>
                    <p className="text-sm text-gray-400">vs {match.opponentUsername}</p>
                  </div>
                  <div className="text-right">
                    <span className={`px-3 py-1 text-xs font-bold rounded-full ${
                        match.result === 'WIN' ? 'bg-green-500/20 text-green-400' :
                        match.result === 'LOSS' ? 'bg-red-500/20 text-red-400' :
                        'bg-yellow-500/20 text-yellow-400'
                      }`}>
                      {match.result}
                    </span>
                    <p className="text-sm text-gray-500 mt-1">{formatDistanceToNow(new Date(match.endedAt))} ago</p>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="p-8 text-center text-gray-500">You haven't played any matches yet. Time to duel!</p>
          )}
        </div>
      </section>
    </div>
  );
};

export default Home;