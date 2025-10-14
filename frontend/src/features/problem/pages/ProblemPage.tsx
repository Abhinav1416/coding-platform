import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import Tabs from '../../../core/components/Tabs';
import { useProblem } from '../hooks/useProblem';
import ProblemDetails from '../components/ProblemDetails';
import CodeEditor from '../components/CodeEditor';
import SubmissionsList from '../components/SubmissionsList';
import SubmissionDetailModal from '../SubmissionDetailModal';
import { createSubmission, getProblemSubmissions, getSubmissionDetails } from '../services/problemService';
import { stompService } from '../../../core/sockets/stompClient';
import type { SubmissionSummary, SubmissionDetails } from '../types/problem';
import MainLayout from '../../../components/layout/MainLayout';



const ProblemStateLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <MainLayout>
        <div className="flex flex-col items-center justify-center text-center pt-24">
            {children}
        </div>
    </MainLayout>
);

const ProblemPage: React.FC = () => {
    const { slug } = useParams<{ slug: string }>();
    const { problem, isLoading, error } = useProblem(slug || '');

    const [language, setLanguage] = useState<'cpp' | 'java' | 'python'>('cpp');
    const [code, setCode] = useState<string>('// Start coding here...');
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    
    const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetails | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);

    const handleRealTimeUpdate = useCallback((result: { status: string; submissionId: string }) => {
        setSubmissions(prevSubs => 
            prevSubs.map(sub => 
                sub.id === result.submissionId ? { ...sub, status: result.status } : sub
            )
        );
    }, []);

    useEffect(() => {
        stompService.connect();
        if (problem) {
            getProblemSubmissions(problem.id).then(response => {
                setSubmissions(response.submissions);
                response.submissions.forEach(sub => {
                    if (sub.status === 'PENDING' || sub.status === 'PROCESSING') {
                        stompService.subscribeToSubmissionResult(sub.id, handleRealTimeUpdate);
                    }
                });
            });
        }
        return () => stompService.disconnect();
    }, [problem, handleRealTimeUpdate]);


    const handleSubmit = async () => {
        if (!problem || isSubmitting) return;

        setIsSubmitting(true);
        setActiveTab(1);
        try {
            const response = await createSubmission({ problemId: problem.id, language, code, matchId: null });
            const newSubmission: SubmissionSummary = {
                id: response.submissionId,
                status: 'PENDING',
                language,
                runtimeMs: null,
                createdAt: new Date().toISOString(),
                matchId: null
            };
            setSubmissions(prev => [newSubmission, ...prev]);
            stompService.subscribeToSubmissionResult(response.submissionId, handleRealTimeUpdate);
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



    if (isLoading) return (
        <ProblemStateLayout>
            <Loader2 className="animate-spin text-[#F97316]" size={48} />
            <p className="mt-4 text-lg text-gray-700 dark:text-gray-400">Loading Problem...</p>
        </ProblemStateLayout>
    );

    if (error) return (
        <ProblemStateLayout>
            <p className="text-red-500 text-xl">{error}</p>
        </ProblemStateLayout>
    );

    if (!problem) return (
        <ProblemStateLayout>
            <p className="text-lg text-gray-800 dark:text-gray-200">Problem not found.</p>
        </ProblemStateLayout>
    );


    const isSubmissionDisabled = problem.status !== 'PUBLISHED' || isSubmitting;

    const leftPanelTabs = [
        { label: 'Description', content: <ProblemDetails problem={problem} /> },
        { label: 'Submissions', content: <SubmissionsList submissions={submissions} onSubmissionClick={handleSubmissionClick} /> },
    ];

    return (
        <>
            <div className="flex flex-col h-screen bg-white dark:bg-[#18181b]">
                <MainLayout>
                    <div className="flex-grow flex flex-col min-h-0 -m-8">
                        <PanelGroup direction="horizontal" className="flex-grow overflow-hidden">
                            <Panel defaultSize={50} minSize={30} className="flex flex-col bg-gray-50 dark:bg-zinc-900">
                                <Tabs tabs={leftPanelTabs} activeTab={activeTab} setActiveTab={setActiveTab} />
                            </Panel>
                            <PanelResizeHandle className="w-2 bg-gray-300 dark:bg-zinc-800 hover:bg-[#F97316]/80 active:bg-[#F97316] transition-colors duration-200" />
                            <Panel defaultSize={50} minSize={30} className="flex flex-col bg-gray-50 dark:bg-zinc-900">
                                <CodeEditor 
                                    language={language} 
                                    setLanguage={setLanguage}
                                    code={code}
                                    setCode={setCode}
                                    onSubmit={handleSubmit}
                                    isSubmittingDisabled={isSubmissionDisabled}
                                />
                            </Panel>
                        </PanelGroup>
                    </div>
                </MainLayout>
            </div>
            
            {isModalOpen && (
                <SubmissionDetailModal 
                    submission={selectedSubmission} 
                    onClose={() => setIsModalOpen(false)} 
                />
            )}
        </>
    );
};

export default ProblemPage;