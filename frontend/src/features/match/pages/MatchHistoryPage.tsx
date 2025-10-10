import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { format } from 'date-fns';
import { getMatchHistory } from '../services/matchService';
import type { Page, PastMatch } from '../types/match';
import { Pagination } from '../../../core/components/Pagination';

const MatchHistoryPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [data, setData] = useState<Page<PastMatch> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const currentFilter = searchParams.get('result') || 'ALL';

  useEffect(() => {
    setIsLoading(true);
    const resultFilter = currentFilter === 'ALL' ? undefined : currentFilter;

    getMatchHistory({ page: currentPage, size: 10, result: resultFilter })
      .then(responseData => {
        setData(responseData);
      })
      .catch(err => {
        console.error("Failed to fetch match history:", err);
        setError("Could not load your match history. Please try again later.");
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [currentPage, currentFilter]);

  const handlePageChange = (page: number) => {
    setSearchParams({ page: page.toString(), result: currentFilter });
  };
  
  const handleFilterChange = (newFilter: string) => {
    setSearchParams({ page: '0', result: newFilter });
  };

  // --- THIS FUNCTION IS UPDATED ---
  const getResultBadgeClass = (result: PastMatch['result']) => {
    switch (result) {
      case 'WIN': 
        return 'bg-green-500/20 text-green-400';
      case 'LOSS': 
        return 'bg-red-500/20 text-red-400';
      case 'DRAW': 
        return 'bg-yellow-500/20 text-yellow-400';
      case 'CANCELED': 
        return 'bg-gray-500/20 text-gray-400';
      case 'EXPIRED': 
        return 'bg-zinc-500/20 text-zinc-400';
      default: 
        return 'bg-gray-500/20 text-gray-400';
    }
  };
  // ---------------------------------

  const FilterButton = ({ filter, label }: { filter: string, label: string }) => (
    <button
      onClick={() => handleFilterChange(filter)}
      className={`px-4 py-2 text-sm font-semibold rounded-md transition-colors ${
        currentFilter === filter
          ? 'bg-[#F97316] text-white'
          : 'bg-zinc-800 text-gray-300 hover:bg-zinc-700'
      }`}
    >
      {label}
    </button>
  );

  return (
    <div className="container mx-auto py-8">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 mb-6">
          <h1 className="text-3xl font-bold text-white">Match History</h1>
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
          <div className="bg-zinc-900 rounded-lg border border-white/10 overflow-hidden">
            <table className="w-full text-left">
              <thead className="bg-zinc-800 text-xs text-gray-400 uppercase">
                <tr>
                  <th className="px-6 py-3">Problem</th>
                  <th className="px-6 py-3">Opponent</th>
                  <th className="px-6 py-3 text-center">Result</th>
                  <th className="px-6 py-3 text-right">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800">
                {data.content.map(match => (
                  <tr key={match.matchId} className="hover:bg-zinc-800/50">
                    <td className="px-6 py-4 font-medium text-white">{match.problemTitle}</td>
                    <td className="px-6 py-4 text-gray-300">{match.opponentUsername}</td>
                    <td className="px-6 py-4 text-center">
                      <span className={`px-3 py-1 text-xs font-bold rounded-full ${getResultBadgeClass(match.result)}`}>
                        {match.result}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right text-gray-400">
                      {/* This line is now correct because the types match */}
                      {format(new Date(match.endedAt || match.createdAt), 'MMM d, yyyy')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination 
            currentPage={data.number} 
            totalPages={data.totalPages} 
            onPageChange={handlePageChange} 
          />
        </>
      ) : (
        <div className="text-center bg-zinc-900 rounded-lg p-12 border border-white/10">
            <h2 className="text-xl font-semibold text-white">No Matches Found</h2>
            <p className="text-gray-500 mt-2">No matches found for the selected filter.</p>
        </div>
      )}
    </div>
  );
};

export default MatchHistoryPage;