import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { format } from 'date-fns';
import { getMatchHistory } from '../services/matchService';
import type { Page, PastMatch } from '../types/match';
import { Pagination } from '../../../core/components/Pagination';
// REMOVED: MainLayout import is no longer needed here.

const MatchHistoryPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  
  const [data, setData] = useState<Page<PastMatch> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const currentFilter = searchParams.get('result') || 'ALL';

  useEffect(() => {
    setIsLoading(true);
    const resultFilter = currentFilter === 'ALL' ? undefined : currentFilter;

    getMatchHistory({ page: currentPage, size: 10, result: resultFilter })
      .then(responseData => setData(responseData))
      .catch(err => {
        console.error("Failed to fetch match history:", err);
        setError("Could not load your match history. Please try again later.");
      })
      .finally(() => setIsLoading(false));
  }, [currentPage, currentFilter]);
  
  const handleMatchClick = (match: PastMatch) => {
    if (match.result === 'CANCELED') return;
    navigate(`/match/results/${match.matchId}`);
  };

  const handlePageChange = (page: number) => {
    setSearchParams(prevParams => {
      prevParams.set('page', page.toString());
      return prevParams;
    });
  };
  
  const handleFilterChange = (newFilter: string) => {
    setSearchParams(prevParams => {
        prevParams.set('page', '0');
        prevParams.set('result', newFilter);
        return prevParams;
    });
  };

  // --- Theme-aware Badge Styles ---
  const getResultBadgeClass = (result: PastMatch['result']) => {
    switch (result) {
      case 'WIN': return 'bg-green-100 text-green-800 dark:bg-green-500/20 dark:text-green-400';
      case 'LOSS': return 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-400';
      case 'DRAW': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-500/20 dark:text-yellow-400';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-500/20 dark:text-gray-400';
    }
  };

  // --- Theme-aware Filter Button Component ---
  const FilterButton = ({ filter, label }: { filter: string, label: string }) => (
    <button
      onClick={() => handleFilterChange(filter)}
      className={`px-4 py-2 text-sm font-semibold rounded-md transition-colors ${
        currentFilter === filter 
        ? 'bg-[#F97316] text-white' 
        : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-zinc-800 dark:text-gray-300 dark:hover:bg-zinc-700'
      }`}
    >
      {label}
    </button>
  );

  return (
    // REMOVED: The <MainLayout> wrapper is gone.
    // ADDED: A React Fragment (<>) to return a single root element.
    <>
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 mb-6">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Match History</h1>
          <div className="flex gap-2">
              <FilterButton filter="ALL" label="All" />
              <FilterButton filter="WIN" label="Wins" />
              <FilterButton filter="LOSS" label="Losses" />
              <FilterButton filter="DRAW" label="Draws" />
          </div>
      </div>
      
      {isLoading ? (
        <div className="flex justify-center items-center h-64"><Loader2 className="animate-spin text-[#F97316]" size={48} /></div>
      ) : error ? (
        <div className="text-center text-red-500">{error}</div>
      ) : data && data.content.length > 0 ? (
        <>
          <div className="bg-white dark:bg-zinc-900 rounded-lg border border-gray-200 dark:border-white/10 overflow-hidden shadow-sm">
            <table className="w-full text-left">
              <thead className="bg-gray-50 dark:bg-zinc-800 text-xs text-gray-500 dark:text-gray-400 uppercase">
                <tr>
                  <th className="px-6 py-3">Problem</th>
                  <th className="px-6 py-3">Opponent</th>
                  <th className="px-6 py-3 text-center">Result</th>
                  <th className="px-6 py-3 text-right">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-zinc-800">
                {data.content.map(match => (
                  <tr key={match.matchId} onClick={() => handleMatchClick(match)} className={`transition-colors ${match.result !== 'CANCELED' ? 'hover:bg-gray-50 dark:hover:bg-zinc-800/50 cursor-pointer' : 'opacity-60'}`}>
                    <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">{match.problemTitle}</td>
                    <td className="px-6 py-4 text-gray-600 dark:text-gray-300">{match.opponentUsername}</td>
                    <td className="px-6 py-4 text-center">
                      <span className={`px-3 py-1 text-xs font-bold rounded-full ${getResultBadgeClass(match.result)}`}>{match.result}</span>
                    </td>
                    <td className="px-6 py-4 text-right text-gray-500 dark:text-gray-400">{format(new Date(match.endedAt || match.createdAt), 'MMM d, yyyy')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.totalPages > 1 && (
             <Pagination currentPage={data.number} totalPages={data.totalPages} onPageChange={handlePageChange} />
          )}
        </>
      ) : (
        <div className="text-center bg-white dark:bg-zinc-900 rounded-lg p-12 border border-gray-200 dark:border-white/10">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">No Matches Found</h2>
          <p className="text-gray-600 dark:text-gray-500 mt-2">No matches found for the selected filter.</p>
        </div>
      )}
    </>
  );
};

export default MatchHistoryPage;