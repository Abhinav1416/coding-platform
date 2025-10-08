import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { Loader2 } from 'lucide-react';

// Hooks
import { useMatchTimer } from '../hooks/useMatchTimer';
import { useAuth } from '../../../core/hooks/useAuth';

// Components
import MatchHeader from '../components/MatchHeader';
import { MatchResultOverlay } from '../components/MatchResultOverlay';
import ProblemDetails from '../../problem/components/ProblemDetails';
import CodeEditor from '../../problem/components/CodeEditor';
import SubmissionsList from '../../problem/components/SubmissionsList';
import SubmissionDetailModal from '../../problem/SubmissionDetailModal';
import Tabs from '../../../core/components/Tabs';

// Services and Types
import { createSubmission, getSubmissionDetails } from '../../problem/services/problemService';
import { getArenaData } from '../services/matchService';
import { stompService } from '../../../core/sockets/stompClient';
import type { ArenaData, MatchEvent, MatchResult } from '../types/match';
import type { SubmissionSummary, SubmissionDetails } from '../../problem/types/problem';

// --- Helper Hooks (Full Implementation) ---

const useArenaData = (matchId: string | undefined) => {
    const [arenaData, setArenaData] = useState<ArenaData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [shouldRedirect, setShouldRedirect] = useState(false);

    useEffect(() => {
        if (!matchId) {
            setError("No Match ID provided.");
            setIsLoading(false);
            return;
        }
        const fetchDetails = async () => {
            try {
                const data = await getArenaData(matchId);
                setArenaData(data);
            } catch (err: any) {
                if (err instanceof AxiosError && err.response?.status === 409) {
                    // 409 Conflict can mean the match is already over
                    setShouldRedirect(true);
                } else {
                    setError(err.message || "Failed to load match data.");
                }
            } finally {
                setIsLoading(false);
            }
        };
        fetchDetails();
    }, [matchId]);

    return { arenaData, isLoading, error, shouldRedirect };
};

const useMatchEvents = (
    matchId: string | undefined, 
    onMatchEnd: (result: MatchResult) => void
) => {
    const handleMatchEvent = useCallback((event: MatchEvent) => {
        if (event.eventType === 'MATCH_END') {
            onMatchEnd(event.result);
        }
    }, [onMatchEnd]);

    useEffect(() => {
        if (!matchId) return;
        stompService.connect();
        const subscription = stompService.subscribeToMatchUpdates(matchId, handleMatchEvent);
        return () => {
            if (subscription) subscription.unsubscribe();
        };
    }, [matchId, handleMatchEvent]);
};

const LoadingSpinner = () => (
    <div className="min-h-screen bg-gray-900 flex flex-col items-center justify-center">
        <Loader2 className="animate-spin text-[#F97316]" size={48} />
        <p className="mt-4 text-white">Loading Arena...</p>
    </div>
);

// --- Main Arena Page Component ---
const MatchArenaPage: React.FC = () => {
    const { matchId } = useParams<{ matchId: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();

    const { arenaData, isLoading, error, shouldRedirect } = useArenaData(matchId);

    const [matchState, setMatchState] = useState<'LOADING' | 'IN_PROGRESS' | 'AWAITING_RESULT' | 'COMPLETED'>('LOADING');
    const [playerUsernames, setPlayerUsernames] = useState({ p1: 'Player 1', p2: 'Player 2' });
    const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
    
    // Local UI State for editor and submissions
    const [language, setLanguage] = useState<'cpp' | 'java' | 'python'>('cpp');
    const [code, setCode] = useState<string>('// Good luck!');
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetails | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);

    // --- Effects ---

    useEffect(() => {
        if (shouldRedirect && matchId) {
            navigate(`/match/results/${matchId}`, { replace: true });
        }
    }, [shouldRedirect, matchId, navigate]);

    useMatchEvents(matchId, (result) => {
        setMatchResult(result);
        setMatchState('COMPLETED');
    });
    
    useEffect(() => {
        if (arenaData) {
            setPlayerUsernames({ p1: arenaData.playerOneUsername, p2: arenaData.playerTwoUsername });
            setMatchState('IN_PROGRESS');
        }
    }, [arenaData]);
    
    const durationInSeconds = (arenaData?.liveState.durationInMinutes || 0) * 60;
    const { timeLeft } = useMatchTimer(durationInSeconds, matchState);

    useEffect(() => {
        if (timeLeft <= 0 && matchState === 'IN_PROGRESS') {
            console.log("Timer hit zero. Awaiting final result from server...");
            setMatchState('AWAITING_RESULT');
        }
    }, [timeLeft, matchState]);

    useEffect(() => {
        if (matchState === 'COMPLETED' && matchId) {
            console.log("Match completed. Redirecting to results page in 4 seconds...");
            const timer = setTimeout(() => {
                navigate(`/match/results/${matchId}`);
            }, 4000);

            return () => clearTimeout(timer);
        }
    }, [matchState, matchId, navigate]);

    // --- Handlers ---

    const handleSubmissionUpdate = useCallback((update: { submissionId: string; status: string }) => {
        setSubmissions(prev => prev.map(sub => sub.id === update.submissionId ? { ...sub, status: update.status } : sub));
    }, []);

    const handleSubmit = async () => {
        if (!arenaData?.problemDetails || isSubmitting || matchState !== 'IN_PROGRESS') return;

        setIsSubmitting(true);
        setActiveTab(1);
        try {
            const response = await createSubmission({
                problemId: arenaData.problemDetails.id,
                language, code, matchId: matchId!,
            });
            const newSubmission: SubmissionSummary = {
                id: response.submissionId, status: 'PENDING', language, runtimeMs: null,
                createdAt: new Date().toISOString(),
                matchId: matchId ?? null
            };
            setSubmissions(prev => [newSubmission, ...prev]);
            stompService.subscribeToSubmissionResult(response.submissionId, handleSubmissionUpdate);
        } catch (err) {
            console.error("Submission failed:", err);
            alert("An error occurred while submitting.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleSubmissionClick = async (submissionId: string) => {
        const details = await getSubmissionDetails(submissionId);
        setSelectedSubmission(details);
        setIsModalOpen(true);
    };

    // --- Render Logic ---

    if (isLoading) return <LoadingSpinner />;
    if (shouldRedirect) return (
        <div className="min-h-screen bg-gray-900 flex items-center justify-center text-white">
            Match already completed. Redirecting to results...
        </div>
    );
    if (error) return <div className="text-red-500 text-center p-8 text-xl">{error}</div>;
    if (!arenaData?.problemDetails) return <div className="text-center p-8">Match data or problem could not be found.</div>;

    const isEditorDisabled = matchState !== 'IN_PROGRESS' || isSubmitting;
    const { problemDetails } = arenaData;

    const leftPanelTabs = [
        { label: 'Description', content: <ProblemDetails problem={problemDetails} /> },
        { label: 'My Submissions', content: <SubmissionsList submissions={submissions} onSubmissionClick={handleSubmissionClick} /> },
    ];

    return (
        <>
            <div className="flex flex-col h-screen bg-gray-900 text-white">
                <MatchHeader
                    timeLeft={timeLeft}
                    playerOneUsername={playerUsernames.p1}
                    playerTwoUsername={playerUsernames.p2}
                    status={matchState}
                />
                <div className="flex flex-grow overflow-hidden">
                    <div className="w-1/2 border-r border-gray-700">
                        <Tabs tabs={leftPanelTabs} activeTab={activeTab} setActiveTab={setActiveTab} />
                    </div>
                    <div className="w-1/2">
                        <CodeEditor
                            language={language} setLanguage={setLanguage}
                            code={code} setCode={setCode}
                            onSubmit={handleSubmit} isSubmittingDisabled={isEditorDisabled}
                        />
                    </div>
                </div>
            </div>
            
            {matchState === 'COMPLETED' && matchResult && (
                <MatchResultOverlay result={matchResult} currentUserEmail={user?.email} />
            )}

            {isModalOpen && (
                <SubmissionDetailModal submission={selectedSubmission} onClose={() => setIsModalOpen(false)} />
            )}
        </>
    );
};

export default MatchArenaPage;