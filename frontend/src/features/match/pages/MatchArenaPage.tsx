import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { Loader2 } from 'lucide-react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';

// Core and Layout components
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
import type { ArenaData, MatchResult } from '../types/match';
import type { SubmissionSummary, SubmissionDetails } from '../../problem/types/problem';
import { useServerTimer } from '../../../core/components/useServerTimer';
import Navbar from '../../../components/layout/Navbar';

// --- (Helper hooks remain unchanged) ---
const useArenaData = (matchId: string | undefined) => {
    // ... (This hook is unchanged)
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

const useMatchEvents = (
    // ... (This hook is unchanged)
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
        stompService.connect();
        const subs = [
            stompService.subscribeToMatchUpdates(matchId, handleMatchEvent),
            stompService.subscribeToCountdown(matchId, handleCountdownEvent)
        ];
        return () => {
            subs.forEach(sub => { if (sub) sub.unsubscribe() });
        };
    }, [matchId, handleMatchEvent, handleCountdownEvent]);
};


// --- Themed Layout for Loading/Error States ---
const ArenaStateLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <div className="flex flex-col h-screen bg-white dark:bg-zinc-950">
        <Navbar />
        <main className="flex-grow flex items-center justify-center text-center p-4">
            {children}
        </main>
    </div>
);


// --- Main Arena Page Component ---
const MatchArenaPage: React.FC = () => {
    const { matchId } = useParams<{ matchId: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();

    const { arenaData, isLoading, error, shouldRedirect } = useArenaData(matchId);

    // ... (All state and hooks from the original component remain unchanged here)
    const [timerData, setTimerData] = useState<{ startTime: number; duration: number } | null>(null);
    const [matchState, setMatchState] = useState<'LOADING' | 'IN_PROGRESS' | 'AWAITING_RESULT' | 'COMPLETED'>('LOADING');
    const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
    const [language, setLanguage] = useState<'cpp' | 'java' | 'python'>('cpp');
    const [code, setCode] = useState<string>('// Loading code...'); 
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetails | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);
    const COOLDOWN_SECONDS = 5;
    const [isCoolingDown, setIsCoolingDown] = useState(false);
    const [cooldownTime, setCooldownTime] = useState(COOLDOWN_SECONDS);
    const cooldownIntervalRef = useRef<number | null>(null);

    // ... (All useEffect hooks from the original component remain unchanged here)
    useEffect(() => {
        if (matchId && user?.email) {
            try {
                const storageKey = `code-cache-${user.email}-${matchId}`;
                const savedCode = localStorage.getItem(storageKey);
                if (savedCode) {
                    setCode(savedCode);
                } else {
                    setCode('// Good luck!');
                }
            } catch (e) {
                console.error("Could not read from localStorage:", e);
                setCode('// Good luck!');
            }
        }
    }, [matchId, user?.email]);
    useEffect(() => {
        if (matchId && user?.email && code !== '// Loading code...') {
            try {
                const storageKey = `code-cache-${user.email}-${matchId}`;
                localStorage.setItem(storageKey, code);
            } catch (e) {
                console.error("Could not write to localStorage:", e);
            }
        }
    }, [code, matchId, user?.email]);
    useEffect(() => {
        if (shouldRedirect && matchId) {
            navigate(`/match/results/${matchId}`, { replace: true });
        }
    }, [shouldRedirect, matchId, navigate]);
    useMatchEvents(matchId,
        (result) => {
            setMatchResult(result);
            setMatchState('COMPLETED');
        },
        (payload) => {
            setTimerData(payload);
        }
    );
    useEffect(() => {
        if (arenaData) {
            setMatchState('IN_PROGRESS');
            const { startedAt, durationInMinutes } = arenaData.liveState;
            if (startedAt && durationInMinutes > 0) {
                setTimerData({
                    startTime: new Date(startedAt).getTime(),
                    duration: durationInMinutes * 60,
                });
            }
        }
    }, [arenaData]);
    const { totalSeconds: timeLeft, isFinished } = useServerTimer(timerData?.startTime ?? null, timerData?.duration ?? null);
    useEffect(() => {
        if (isFinished && matchState === 'IN_PROGRESS' && timerData) {
            setMatchState('AWAITING_RESULT');
        }
    }, [isFinished, matchState, timerData]);
    useEffect(() => {
        if (matchState === 'COMPLETED' && matchId) {
            const timer = setTimeout(() => {
                navigate(`/match/results/${matchId}`);
            }, 4000);
            return () => clearTimeout(timer);
        }
    }, [matchState, matchId, navigate]);
    useEffect(() => {
        if (isCoolingDown) {
            cooldownIntervalRef.current = window.setInterval(() => {
                setCooldownTime(prevTime => {
                    if (prevTime <= 1) {
                        if (cooldownIntervalRef.current) {
                            clearInterval(cooldownIntervalRef.current);
                        }
                        setIsCoolingDown(false);
                        return COOLDOWN_SECONDS;
                    }
                    return prevTime - 1;
                });
            }, 1000);
        }
        return () => {
            if (cooldownIntervalRef.current) {
                clearInterval(cooldownIntervalRef.current);
            }
        };
    }, [isCoolingDown]);

    const handleSubmissionUpdate = useCallback((update: { submissionId: string; status: string }) => {
        setSubmissions(prev => prev.map(sub => sub.id === update.submissionId ? { ...sub, status: update.status } : sub));
    }, []);

    const handleSubmit = async () => {
        if (!arenaData?.problemDetails || isSubmitting || isCoolingDown || matchState !== 'IN_PROGRESS') return;
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
        } finally {
            setIsSubmitting(false);
            setIsCoolingDown(true);
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

    // --- Themed Render Logic ---

    if (isLoading) return (
        <ArenaStateLayout>
            <div>
                <Loader2 className="animate-spin text-[#F97316] mx-auto" size={48} />
                <p className="mt-4 text-lg animate-pulse text-gray-600 dark:text-gray-400">Loading Arena...</p>
            </div>
        </ArenaStateLayout>
    );

    if (shouldRedirect) return (
        <ArenaStateLayout>
            <p className="text-lg text-gray-800 dark:text-gray-200">Match already completed. Redirecting...</p>
        </ArenaStateLayout>
    );
    
    if (error) return (
        <ArenaStateLayout>
            <p className="text-red-500 text-xl">{error}</p>
        </ArenaStateLayout>
    );
    
    if (!arenaData?.problemDetails) return (
        <ArenaStateLayout>
            <p className="text-lg text-gray-800 dark:text-gray-200">Match data or problem could not be found.</p>
        </ArenaStateLayout>
    );


    const isEditorDisabled = matchState !== 'IN_PROGRESS' || isSubmitting || isCoolingDown;
    const { problemDetails, playerOneUsername, playerTwoUsername } = arenaData;

    const leftPanelTabs = [
        { label: 'Description', content: <ProblemDetails problem={problemDetails} /> },
        { label: 'My Submissions', content: <SubmissionsList submissions={submissions} onSubmissionClick={handleSubmissionClick} /> },
    ];

    let submitButtonText = "Submit";
    if (isSubmitting) submitButtonText = "Submitting...";
    else if (isCoolingDown) submitButtonText = `Wait ${cooldownTime}s`;

    return (
        <>
            <div className="flex flex-col h-screen bg-white dark:bg-zinc-950 text-gray-900 dark:text-white">
                <Navbar />
                <div className="flex-grow flex flex-col min-h-0">
                    <MatchHeader
                        timeLeft={timeLeft}
                        playerOneUsername={playerOneUsername}
                        playerTwoUsername={playerTwoUsername}
                        status={matchState}
                    />
                    <PanelGroup direction="horizontal" className="flex-grow overflow-hidden">
                        <Panel defaultSize={50} minSize={25} className="flex flex-col bg-gray-50 dark:bg-zinc-900">
                            <Tabs tabs={leftPanelTabs} activeTab={activeTab} setActiveTab={setActiveTab} />
                        </Panel>
                        <PanelResizeHandle className="w-2 bg-gray-300 dark:bg-zinc-800 hover:bg-[#F97316]/80 active:bg-[#F97316] transition-colors duration-200" />
                        <Panel defaultSize={50} minSize={35} className="flex flex-col bg-gray-50 dark:bg-zinc-900">
                            <CodeEditor
                                language={language} setLanguage={setLanguage}
                                code={code} setCode={setCode}
                                onSubmit={handleSubmit}
                                isSubmittingDisabled={isEditorDisabled}
                                submitButtonText={submitButtonText}
                            />
                        </Panel>
                    </PanelGroup>
                </div>
            </div>
            
            {/* Overlays are kept dark for focus, which is standard UI practice */}
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