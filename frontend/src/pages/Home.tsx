import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake, FaAngleRight, FaCode, FaSignInAlt } from 'react-icons/fa';
import { Loader2, Swords, Zap, Users } from 'lucide-react';
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
        console.error("Failed to load stats", err);
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

  const username = user?.email ? user.email.split('@')[0] : "Coder";

  return (
    <div className="space-y-10 pb-20">
      
      <div className="flex flex-col md:flex-row justify-between items-end gap-4">
        <div>
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 dark:text-white">
            Welcome, <span className="text-[#F97316] capitalize">{username}</span>
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-2 text-lg">
            Choose your arena and start coding.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="relative overflow-hidden bg-gradient-to-br from-orange-50 to-orange-100 dark:from-zinc-900 dark:to-zinc-800 rounded-2xl p-8 border border-orange-200 dark:border-zinc-700 shadow-sm transition-all hover:shadow-md">
          <div className="absolute top-0 right-0 p-4 opacity-10 dark:opacity-5">
            <Swords size={120} className="text-[#F97316]" />
          </div>
          <div className="relative z-10">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-orange-200 dark:bg-orange-900/30 text-orange-700 dark:text-orange-400 text-xs font-bold mb-4">
              <Zap size={12} /> NEW FEATURE
            </div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3 mb-2">
              <Swords className="text-[#F97316]" /> Codeforces Duel
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mb-8 max-w-sm">
              Real-time 1v1 battles using live Codeforces problems. Official ICPC scoring rules.
            </p>
            <div className="flex flex-col sm:flex-row gap-4">
              <button onClick={() => navigate('/duel/create')} className="flex-1 bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-6 rounded-lg shadow-lg shadow-orange-500/20 hover:shadow-orange-500/30 transition-all transform hover:scale-105">
                Create Room
              </button>
              <button onClick={() => navigate('/duel/join')} className="flex-1 bg-white dark:bg-zinc-800 text-gray-800 dark:text-gray-200 font-bold py-3 px-6 rounded-lg border border-gray-200 dark:border-zinc-600 hover:bg-gray-50 dark:hover:bg-zinc-700 transition-colors">
                Join Room
              </button>
            </div>
          </div>
        </div>

        <div className="relative overflow-hidden bg-white dark:bg-zinc-900 rounded-2xl p-8 border border-gray-200 dark:border-zinc-800 shadow-sm transition-all hover:shadow-md">
          <div className="absolute top-0 right-0 p-4 opacity-5">
            <FaGamepad size={120} />
          </div>
          <div className="relative z-10">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-gray-100 dark:bg-zinc-800 text-gray-600 dark:text-gray-400 text-xs font-bold mb-4">
              <Users size={12} /> CLASSIC
            </div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3 mb-2">
              <FaCode className="text-blue-500" /> Standard Match
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mb-8 max-w-sm">
              Practice with our internal problem library. Good for warmups.
            </p>
            <div className="flex flex-col sm:flex-row gap-4">
              <button onClick={() => navigate('/match/create')} className="flex-1 bg-gray-900 dark:bg-zinc-700 hover:bg-gray-800 dark:hover:bg-zinc-600 text-white font-bold py-3 px-6 rounded-lg transition-all">
                Create Match
              </button>
              <button onClick={() => navigate('/match/join')} className="flex-1 bg-white dark:bg-zinc-800 text-gray-800 dark:text-gray-200 font-bold py-3 px-6 rounded-lg border border-gray-200 dark:border-zinc-600 hover:bg-gray-50 dark:hover:bg-zinc-700 transition-colors">
                Join Match
              </button>
            </div>
          </div>
        </div>
      </div>

      {user ? (
        <>
          {stats && (
            <section>
              <h2 className="text-xl font-bold mb-6 text-gray-900 dark:text-white">Your Performance</h2>
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
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">Recent Activity</h2>
              <Link to='/matches/history' className="text-[#F97316] hover:text-[#EA580C] font-semibold flex items-center gap-1 transition-colors text-sm">
                View All <FaAngleRight />
              </Link>
            </div>
            <div className="bg-white dark:bg-zinc-900 rounded-xl border border-gray-200 dark:border-zinc-800 shadow-sm overflow-hidden">
              {recentMatches.length > 0 ? (
                <ul className="divide-y divide-gray-200 dark:divide-zinc-800">
                  {recentMatches.map((match) => {
                    const resultInfo = getResultInfo(match.result);
                    const displayDate = match.endedAt || match.createdAt;
                    return (
                      <li key={match.matchId} className="p-4 sm:p-5 flex justify-between items-center hover:bg-gray-50 dark:hover:bg-zinc-800/50 transition-colors">
                        <div className="flex items-center gap-4">
                          <div className={`p-2 rounded-lg ${match.result === 'WIN' ? 'bg-green-100 dark:bg-green-900/20 text-green-600' : 'bg-gray-100 dark:bg-zinc-800 text-gray-500'}`}>
                              <FaTrophy size={16} />
                          </div>
                          <div>
                              <p className="font-semibold text-gray-900 dark:text-white line-clamp-1">{match.problemTitle}</p>
                              <p className="text-xs text-gray-500 dark:text-gray-400">vs {match.opponentUsername}</p>
                          </div>
                        </div>
                        <div className="text-right flex-shrink-0 ml-4">
                          <span className={`px-2 py-1 text-[10px] font-bold rounded uppercase tracking-wider ${resultInfo.className}`}>
                            {resultInfo.text}
                          </span>
                          <p className="text-xs text-gray-400 mt-1">{formatDistanceToNow(new Date(displayDate))} ago</p>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <div className="p-12 text-center text-gray-500">
                  <FaGamepad className="mx-auto text-4xl mb-4 opacity-20" />
                  <p>No matches played yet.</p>
                </div>
              )}
            </div>
          </section>
        </>
      ) : (
        <section className="bg-gray-100 dark:bg-zinc-900 rounded-xl p-8 text-center border border-gray-200 dark:border-zinc-800">
           <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-2">Track Your Progress</h2>
           <p className="text-gray-500 dark:text-gray-400 mb-6">Login to see your detailed match history, win rates, and problem stats.</p>
           <button onClick={() => navigate('/login')} className="inline-flex items-center gap-2 bg-gray-900 dark:bg-zinc-700 hover:bg-gray-800 text-white font-bold py-2 px-6 rounded-lg transition-all">
             <FaSignInAlt /> Login to Dashboard
           </button>
        </section>
      )}

    </div>
  );
};

export default Home;