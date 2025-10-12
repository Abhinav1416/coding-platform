import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { FaTrophy, FaTimesCircle, FaHandshake } from 'react-icons/fa';

import type { MatchResult, UserStats, PlayerResult as ApiPlayerResult } from '../types/match';
import { getMatchResult, getPlayerStats } from '../services/matchService';
import { getSubmissionDetails } from '../../problem/services/problemService';
import type { SubmissionDetails } from '../../problem/types/problem';
import MainLayout from '../../../components/layout/MainLayout';

// --- Sub-Component for displaying a player's updated total stats ---
const StatsDisplay = ({ stats }: { stats: UserStats }) => (
    <div className="bg-gray-100 dark:bg-zinc-800 p-4 rounded-lg mt-4">
        <h4 className="font-bold text-lg text-gray-900 dark:text-white mb-2">Updated Total Stats</h4>
        <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm text-gray-600 dark:text-gray-400">
            <p>Played: <span className="font-bold text-gray-800 dark:text-white">{stats.duelsPlayed}</span></p>
            <p>Won: <span className="font-bold text-green-600 dark:text-green-400">{stats.duelsWon}</span></p>
            <p>Lost: <span className="font-bold text-red-600 dark:text-red-500">{stats.duelsLost}</span></p>
            <p>Drawn: <span className="font-bold text-yellow-600 dark:text-yellow-400">{stats.duelsDrawn}</span></p>
        </div>
    </div>
);

// --- Sub-Component for displaying the winning code submission ---
const WinningSubmission = ({ submission }: { submission: SubmissionDetails }) => (
    <div className="bg-white dark:bg-zinc-900 border border-green-300 dark:border-green-500/50 rounded-xl p-6 mt-8 shadow-lg">
        <h2 className="text-2xl font-bold text-center text-green-600 dark:text-green-400 mb-4">Winning Submission</h2>
        <pre className="bg-gray-100 dark:bg-zinc-800 text-gray-800 dark:text-gray-200 font-mono p-4 rounded-md overflow-x-auto text-sm">
            <code>{submission.code}</code>
        </pre>
        <div className="flex justify-between mt-2 text-xs text-gray-500 dark:text-gray-500">
            <span>Language: {submission.language}</span>
            <span>Runtime: {submission.runtimeMs}ms</span>
        </div>
    </div>
);


// --- Main Results Page Component ---
const MatchResultsPage = () => {
    const { matchId } = useParams<{ matchId:string }>();
    const [result, setResult] = useState<MatchResult | null>(null);
    const [stats, setStats] = useState<{ p1: UserStats | null, p2: UserStats | null }>({ p1: null, p2: null });
    const [winningSubmission, setWinningSubmission] = useState<SubmissionDetails | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [retryCount, setRetryCount] = useState(0);

    const calculateScore = (player: ApiPlayerResult): number => {
        if (!player.solved) return 0;
        const baseScore = 100;
        const penaltyCost = 10;
        return Math.max(0, baseScore - (player.penalties * penaltyCost));
    };

    useEffect(() => {
        if (!matchId) {
            setError("No Match ID provided.");
            setIsLoading(false);
            return;
        }

        const fetchAllResults = async () => {
            setError(null);
            setIsLoading(true); // Set loading to true at the start of fetch
            try {
                const apiResult = await getMatchResult(matchId);
                const [p1Stats, p2Stats] = await Promise.all([
                    getPlayerStats(apiResult.playerOne.userId),
                    getPlayerStats(apiResult.playerTwo.userId)
                ]);
                setStats({ p1: p1Stats, p2: p2Stats });

                if (apiResult.winningSubmissionId) {
                    const submissionDetails = await getSubmissionDetails(apiResult.winningSubmissionId);
                    setWinningSubmission(submissionDetails);
                }

                const finalResult: MatchResult = {
                    ...apiResult,
                    playerOne: { ...apiResult.playerOne, username: apiResult.playerOne.username || 'Player 1', score: calculateScore(apiResult.playerOne) },
                    playerTwo: { ...apiResult.playerTwo, username: apiResult.playerTwo.username || 'Player 2', score: calculateScore(apiResult.playerTwo) }
                };
                
                setResult(finalResult);
            } catch (err) {
                if (retryCount < 3) {
                    setTimeout(() => setRetryCount(prevCount => prevCount + 1), 1500);
                } else {
                    setError("Failed to load match results. The match may not be complete.");
                }
            } finally {
                setIsLoading(false);
            }
        };

        fetchAllResults();
    }, [matchId, retryCount]);


    if (isLoading) {
        return (
            <MainLayout>
                <div className="flex flex-col items-center justify-center pt-24">
                    <Loader2 className="animate-spin text-[#F97136]" size={48} />
                    <p className="mt-4 text-gray-700 dark:text-gray-300">Fetching Final Results...</p>
                </div>
            </MainLayout>
        );
    }
    
    if (error || !result) {
        return (
            <MainLayout>
                <div className="flex flex-col items-center justify-center pt-24 text-center">
                    <h2 className="text-2xl font-bold text-red-500">Error</h2>
                    <p className="text-gray-600 dark:text-gray-400 mt-2">{error || "Could not display match results."}</p>
                </div>
            </MainLayout>
        );
    }

    const outcomeConfig = result.winnerId 
        ? (result.playerOne.userId === result.winnerId ? { p1: 'WIN', p2: 'LOSS' } : { p1: 'LOSS', p2: 'WIN' }) 
        : { p1: 'DRAW', p2: 'DRAW' };

    return (
        <MainLayout>
            <div className="container mx-auto max-w-4xl">
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold text-gray-900 dark:text-white">Match Results</h1>
                    <p className="text-gray-600 dark:text-gray-400 mt-2">{result.winnerUsername ? `${result.winnerUsername} is victorious!` : "The match ended in a draw."}</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    {/* Player One Results Card */}
                    <div className={`bg-white dark:bg-zinc-900 border border-gray-200 dark:border-white/10 p-6 rounded-xl ${outcomeConfig.p1 === 'WIN' && 'border-green-400 dark:border-green-500 shadow-lg shadow-green-500/10'}`}>
                        <div className="flex justify-between items-center">
                            <h3 className="text-2xl font-bold text-gray-900 dark:text-white capitalize">{result.playerOne.username}</h3>
                            {outcomeConfig.p1 === 'WIN' ? <FaTrophy className="text-2xl text-green-500 dark:text-green-400" /> : outcomeConfig.p1 === 'LOSS' ? <FaTimesCircle className="text-2xl text-red-500" /> : <FaHandshake className="text-2xl text-yellow-500 dark:text-yellow-400" />}
                        </div>
                        <p className="text-3xl font-bold text-[#F97316] mt-2">{result.playerOne.score} pts</p>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Penalties: {result.playerOne.penalties}</p>
                        {stats.p1 && <StatsDisplay stats={stats.p1} />}
                    </div>

                    {/* Player Two Results Card */}
                    <div className={`bg-white dark:bg-zinc-900 border border-gray-200 dark:border-white/10 p-6 rounded-xl ${outcomeConfig.p2 === 'WIN' && 'border-green-400 dark:border-green-500 shadow-lg shadow-green-500/10'}`}>
                        <div className="flex justify-between items-center">
                            <h3 className="text-2xl font-bold text-gray-900 dark:text-white capitalize">{result.playerTwo.username}</h3>
                            {outcomeConfig.p2 === 'WIN' ? <FaTrophy className="text-2xl text-green-500 dark:text-green-400" /> : outcomeConfig.p2 === 'LOSS' ? <FaTimesCircle className="text-2xl text-red-500" /> : <FaHandshake className="text-2xl text-yellow-500 dark:text-yellow-400" />}
                        </div>
                        <p className="text-3xl font-bold text-[#F97316] mt-2">{result.playerTwo.score} pts</p>
                        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">Penalties: {result.playerTwo.penalties}</p>
                        {stats.p2 && <StatsDisplay stats={stats.p2} />}
                    </div>
                </div>

                {winningSubmission && <WinningSubmission submission={winningSubmission} />}
                
                <div className="text-center mt-12">
                    <Link to="/home" className="bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-8 rounded-md transition-transform transform hover:scale-105">
                        Return to Home
                    </Link>
                </div>
            </div>
        </MainLayout>
    );
};

export default MatchResultsPage;