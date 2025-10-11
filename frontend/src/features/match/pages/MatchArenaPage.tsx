import React, { useState, useCallback, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { Loader2 } from 'lucide-react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';

// Core hooks for timing and authentication
import { useAuth } from '../../../core/hooks/useAuth';

// UI Components for the arena
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
import { useServerTimer } from '../../../core/components/useServerTimer';

// --- Helper Hook: Fetches initial arena data via HTTP ---
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

// --- Helper Hook: Manages WebSocket subscriptions and events ---
const useMatchEvents = (
    matchId: string | undefined,
    onMatchEnd: (result: MatchResult) => void,
    onCountdownStart: (data: { startTime: number; duration: number }) => void
) => {
    const handleMatchEvent = useCallback((event: any) => {
        if (event.eventType === 'MATCH_END') {
            onMatchEnd(event.result);
        }
    }, [onMatchEnd]);

    const handleCountdownEvent = useCallback((event: any) => {
        if (event.eventType === 'MATCH_COUNTDOWN_STARTED') {
            onCountdownStart(event.payload);
        }
    }, [onCountdownStart]);

    useEffect(() => {
        if (!matchId) return;

        // Ensure the WebSocket connection is initiated. The robust service handles queuing.
        stompService.connect();

        const subs = [
            stompService.subscribeToMatchUpdates(matchId, handleMatchEvent),
            stompService.subscribeToCountdown(matchId, handleCountdownEvent)
        ];
        
        // Cleanup subscriptions when the component unmounts
        return () => {
            subs.forEach(sub => { if (sub) sub.unsubscribe() });
        };
    }, [matchId, handleMatchEvent, handleCountdownEvent]);
};

// --- Helper Component: Loading Spinner ---
const LoadingSpinner = () => (
    <div className="min-h-screen bg-zinc-950 flex flex-col items-center justify-center text-gray-400">
        <Loader2 className="animate-spin text-[#F97316]" size={48} />
        <p className="mt-4 text-lg animate-pulse">Loading Arena...</p>
    </div>
);

// --- Main Arena Page Component ---
const MatchArenaPage: React.FC = () => {
    const { matchId } = useParams<{ matchId: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();

    const { arenaData, isLoading, error, shouldRedirect } = useArenaData(matchId);

    const [timerData, setTimerData] = useState<{ startTime: number; duration: number } | null>(null);
    const [matchState, setMatchState] = useState<'LOADING' | 'IN_PROGRESS' | 'AWAITING_RESULT' | 'COMPLETED'>('LOADING');
    const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
    
    // Editor and submission related state
    const [language, setLanguage] = useState<'cpp' | 'java' | 'python'>('cpp');
    const [code, setCode] = useState<string>('// Good luck!');
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetails | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);

    // Redirect to results if the match is already completed
    useEffect(() => {
        if (shouldRedirect && matchId) {
            navigate(`/match/results/${matchId}`, { replace: true });
        }
    }, [shouldRedirect, matchId, navigate]);

    // Set up WebSocket event listeners
    useMatchEvents(matchId,
        (result) => {
            setMatchResult(result);
            setMatchState('COMPLETED');
        },
        (payload) => {
            // This is still useful if the timer needs to start after the page has loaded
            setTimerData(payload);
        }
    );
    
    // =================================================================================
    // MODIFIED CODE BLOCK
    // =================================================================================
    // Transition to IN_PROGRESS and initialize the timer from the HTTP response
    useEffect(() => {
        if (arenaData) {
            setMatchState('IN_PROGRESS');

            // --- FIX: Initialize timer from the authoritative HTTP data ---
            // This prevents the race condition where the WebSocket message might be missed on initial load.
            const { startedAt, durationInMinutes } = arenaData.liveState;

            if (startedAt && durationInMinutes > 0) {
                setTimerData({
                    // Convert the ISO 8601 string from the server into a UTC millisecond timestamp
                    startTime: new Date(startedAt).getTime(),
                    // Convert the duration from minutes to seconds, as required by useServerTimer
                    duration: durationInMinutes * 60,
                });
            }
            // --- END OF FIX ---
        }
    }, [arenaData]);
    // =================================================================================
    
    // The new, self-correcting timer hook
    const { totalSeconds: timeLeft, isFinished } = useServerTimer(timerData?.startTime ?? null, timerData?.duration ?? null);

    // Transition to AWAITING_RESULT when the timer hits zero
    useEffect(() => {
        if (isFinished && matchState === 'IN_PROGRESS' && timerData) {
            setMatchState('AWAITING_RESULT');
        }
    }, [isFinished, matchState, timerData]);

    // Automatically navigate to results page after match completion
    useEffect(() => {
        if (matchState === 'COMPLETED' && matchId) {
            const timer = setTimeout(() => {
                navigate(`/match/results/${matchId}`);
            }, 4000);
            return () => clearTimeout(timer);
        }
    }, [matchState, matchId, navigate]);

    // Submission-related handlers
    const handleSubmissionUpdate = useCallback((update: { submissionId: string; status: string }) => {
        setSubmissions(prev => prev.map(sub => sub.id === update.submissionId ? { ...sub, status: update.status } : sub));
    }, []);

    const handleSubmit = async () => {
        if (!arenaData?.problemDetails || isSubmitting || matchState !== 'IN_PROGRESS') return;
        setIsSubmitting(true);
        setActiveTab(1); // Switch to "My Submissions" tab
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
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleSubmissionClick = async (submissionId: string) => {
        try {
            const details = await getSubmissionDetails(submissionId);
            setSelectedSubmission(details);
            setIsModalOpen(true);
        } catch (err) {
            console.error("Failed to fetch submission details:", err);
        }
    };

    // Render loading/error states
    if (isLoading) return <LoadingSpinner />;
    if (shouldRedirect) return <div className="min-h-screen bg-gray-900 flex items-center justify-center text-white">Match already completed. Redirecting...</div>;
    if (error) return <div className="text-red-500 text-center p-8 text-xl">{error}</div>;
    if (!arenaData?.problemDetails) return <div className="text-center p-8">Match data or problem could not be found.</div>;

    const isEditorDisabled = matchState !== 'IN_PROGRESS' || isSubmitting;
    const { problemDetails, playerOneUsername, playerTwoUsername } = arenaData;

    const leftPanelTabs = [
        { label: 'Description', content: <ProblemDetails problem={problemDetails} /> },
        { label: 'My Submissions', content: <SubmissionsList submissions={submissions} onSubmissionClick={handleSubmissionClick} /> },
    ];

    return (
        <>
            <div className="flex flex-col h-screen bg-zinc-950 text-white">
                <MatchHeader
                    timeLeft={timeLeft}
                    playerOneUsername={playerOneUsername}
                    playerTwoUsername={playerTwoUsername}
                    status={matchState}
                />
                <PanelGroup direction="horizontal" className="flex-grow overflow-hidden">
                    <Panel defaultSize={50} minSize={25} className="flex flex-col bg-zinc-900">
                        <Tabs tabs={leftPanelTabs} activeTab={activeTab} setActiveTab={setActiveTab} />
                    </Panel>
                    <PanelResizeHandle className="w-2 bg-zinc-800 hover:bg-[#F97316]/80 active:bg-[#F97316] transition-colors duration-200" />
                    <Panel defaultSize={50} minSize={35} className="flex flex-col bg-zinc-900">
                        <CodeEditor
                            language={language} setLanguage={setLanguage}
                            code={code} setCode={setCode}
                            onSubmit={handleSubmit} isSubmittingDisabled={isEditorDisabled}
                        />
                    </Panel>
                </PanelGroup>
            </div>
            
            {matchState === 'AWAITING_RESULT' && (
                <div className="absolute inset-0 bg-black/80 backdrop-blur-sm flex flex-col items-center justify-center z-50 transition-opacity duration-300">
                    <Loader2 className="animate-spin text-[#F97316]" size={64} />
                    <h2 className="text-4xl font-bold text-white mt-8">Time's Up!</h2>
                    <p className="text-xl text-gray-400 mt-2 animate-pulse">Calculating final results...</p>
                </div>
            )}
            
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