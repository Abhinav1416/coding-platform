import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake, FaAngleRight } from 'react-icons/fa';
import { Loader2 } from 'lucide-react';
import { useAuth } from '../core/hooks/useAuth';
import { getCurrentUserStats, getMatchHistory } from '../features/match/services/matchService';
import { getProblemCount } from '../features/problem/services/problemService';
import type { UserStats, PastMatch } from '../features/match/types/match';
import { formatDistanceToNow } from 'date-fns';


const StatCard: React.FC<{ to: string; icon: React.ReactNode; label: string; value: number | string }> = ({ to, icon, label, value }) => (
  <Link 
    to={to} 
    className="group bg-white dark:bg-zinc-900 p-6 rounded-xl border border-gray-200 dark:border-zinc-800 flex items-center gap-5 transition-all duration-300 hover:-translate-y-1 hover:shadow-lg hover:border-b-4 hover:border-b-[#F97316] shadow-sm"
  >
    <div className="text-[#F97316] text-3xl transition-transform group-hover:scale-110">
      {icon}
    </div>
    <div>
      <p className="text-sm text-gray-600 dark:text-gray-400">{label}</p>
      <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
    </div>
  </Link>
);

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
          getProblemCount(),
        ]);
        setStats(statsData);
        setRecentMatches(historyData.content);
        setProblemCount(problemCountData.totalCount);
      } catch (err) {
        setError("Failed to load dashboard data. Please try again later.");
      } finally {
        setIsLoading(false);
      }
    };
    fetchData();
  }, [user]);

  const getResultInfo = (result: PastMatch['result']) => {
    switch (result) {
      case 'WIN': return { className: 'bg-green-100 text-green-800 dark:bg-green-500/20 dark:text-green-400', text: 'WIN' };
      case 'LOSS': return { className: 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-400', text: 'LOSS' };
      case 'DRAW': return { className: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-500/20 dark:text-yellow-400', text: 'DRAW' };
      default: return { className: 'bg-gray-100 text-gray-800 dark:bg-gray-500/20 dark:text-gray-400', text: result };
    }
  };

  if (isLoading) {
    return <div className="flex justify-center items-center pt-24"><Loader2 className="animate-spin text-[#F97316]" size={48} /></div>;
  }

  if (error) {
    return <div className="text-center text-red-500 pt-24">{error}</div>;
  }

  return (
    <div className="space-y-12">
      <section className="text-center bg-gradient-to-br from-gray-50 to-gray-200 dark:from-zinc-900 dark:to-zinc-800 p-8 md:p-12 rounded-xl border border-gray-200 dark:border-zinc-800">
        <h1 className="text-4xl md:text-5xl font-bold text-gray-900 dark:text-white mb-3">
          Welcome Back, <span className="capitalize">{user?.email.split('@')[0]}</span>!
        </h1>
        <p className="text-lg text-gray-600 dark:text-gray-400 mb-8 max-w-2xl mx-auto">
          Ready to sharpen your skills? Test yourself against our library of <strong className="text-gray-800 dark:text-white font-semibold">{problemCount}</strong> challenges or start a new match.
        </p>
        <div className="flex flex-col sm:flex-row justify-center items-center gap-4">
          <button onClick={() => navigate('/match/create')} className="w-full sm:w-auto bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-6 rounded-lg shadow-lg shadow-orange-500/20 hover:shadow-orange-500/30 transition-all transform hover:scale-105">
            Start New Match
          </button>
          <button 
            onClick={() => navigate('/match/join')}
            className="w-full sm:w-auto bg-white dark:bg-zinc-700/50 hover:bg-gray-200 dark:hover:bg-zinc-700 text-gray-700 dark:text-gray-200 font-semibold py-3 px-6 border border-gray-300 dark:border-zinc-700 rounded-lg transition-colors"
          >
            Join a Match
          </button>
        </div>
      </section>
      {stats && (
        <section>
          <h2 className="text-2xl font-semibold mb-6 text-gray-900 dark:text-white">Your Stats</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
            <StatCard to="/matches/history?result=ALL" icon={<FaGamepad />} label="Played" value={stats.duelsPlayed} />
            <StatCard to="/matches/history?result=WIN" icon={<FaTrophy />} label="Won" value={stats.duelsWon} />
            <StatCard to="/matches/history?result=LOSS" icon={<FaTimesCircle />} label="Lost" value={stats.duelsLost} />
            <StatCard to="/matches/history?result=DRAW" icon={<FaHandshake />} label="Drawn" value={stats.duelsDrawn} />
          </div>
        </section>
      )}
      <section>
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold text-gray-900 dark:text-white">Recent Matches</h2>
          <Link to='/matches/history' className="text-[#F97316] hover:text-[#EA580C] font-semibold flex items-center gap-1 transition-colors">
            View All <FaAngleRight />
          </Link>
        </div>
        <div className="bg-white dark:bg-zinc-900 rounded-xl border border-gray-200 dark:border-zinc-800 shadow-sm">
          {recentMatches.length > 0 ? (
            <ul className="divide-y divide-gray-200 dark:divide-zinc-800">
              {recentMatches.map((match) => {
                const resultInfo = getResultInfo(match.result);
                const displayDate = match.endedAt || match.createdAt;
                return (
                  <li key={match.matchId} className="p-4 sm:p-6 flex justify-between items-center hover:bg-gray-50 dark:hover:bg-zinc-800/50 transition-colors">
                    <div>
                      <p className="font-semibold text-gray-900 dark:text-white">{match.problemTitle}</p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">vs {match.opponentUsername}</p>
                    </div>
                    <div className="text-right flex-shrink-0 ml-4">
                      <span className={`px-3 py-1 text-xs font-bold rounded-full ${resultInfo.className}`}>
                        {resultInfo.text}
                      </span>
                      <p className="text-sm text-gray-500 mt-1">{formatDistanceToNow(new Date(displayDate))} ago</p>
                    </div>
                  </li>
                );
              })}
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