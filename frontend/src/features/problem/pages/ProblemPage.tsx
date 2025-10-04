import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useProblem } from '../hooks/useProblem';
import { useTimer } from '../hooks/useTimer';
import ProblemDetails from '../components/ProblemDetails';
import CodeEditor from '../components/CodeEditor';
import SubmissionsList from '../components/SubmissionsList';

import Tabs from '../../../core/components/Tabs';
import { createSubmission, getProblemSubmissions, getSubmissionDetails } from '../services/problemService';
import { stompService } from '../../../core/sockets/stompClient';
import type { SubmissionSummary, SubmissionDetails } from '../types/problem';
import SubmissionDetailModal from '../SubmissionDetailModal';

const LoadingSpinner = () => (
    <div className="flex justify-center items-center h-full">
        <div className="animate-spin rounded-full h-32 w-32 border-t-2 border-b-2 border-blue-500"></div>
    </div>
);

const ProblemPage: React.FC = () => {
    const { slug } = useParams<{ slug: string }>();
    const { problem, isLoading, error } = useProblem(slug || '');
    const timer = useTimer();

    const [language, setLanguage] = useState<'cpp' | 'java' | 'python'>('cpp');
    const [code, setCode] = useState<string>('// Start coding here...');
    const [submissions, setSubmissions] = useState<SubmissionSummary[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    
    const [selectedSubmission, setSelectedSubmission] = useState<SubmissionDetails | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const [activeTab, setActiveTab] = useState(0);

    const handleRealTimeUpdate = useCallback((result: { status: string; submissionId: string }) => {
        console.log("!!!!!!!!!! WEB SOCKET MESSAGE RECEIVED !!!!!!!!!!");
        console.log("Received data:", result);
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
        return () => {
            stompService.disconnect();
        };
    }, [problem, handleRealTimeUpdate]);


    const handleSubmit = async () => {
        if (!problem || isSubmitting) return;

        setIsSubmitting(true);
        setActiveTab(1);
        try {
            const response = await createSubmission({
                problemId: problem.id,
                language,
                code,
                matchId: null,
            });

            const { submissionId } = response;
            
            const newSubmission: SubmissionSummary = {
                id: submissionId,
                status: 'PENDING',
                language: language.charAt(0).toUpperCase() + language.slice(1),
                runtimeMs: null,
                createdAt: new Date().toISOString(),
                matchId: null
            };
            setSubmissions(prev => [newSubmission, ...prev]);
            
            stompService.subscribeToSubmissionResult(submissionId, handleRealTimeUpdate);

        } catch (err) {
            console.error("Submission failed:", err);
            alert("An error occurred while submitting.");
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
            alert("Could not load submission details.");
        }
    };

    if (isLoading) return <LoadingSpinner />;
    if (error) return <div className="text-red-500 text-center p-8">{error}</div>;
    if (!problem) return <div className="text-center p-8">Problem not found.</div>;

    const isSubmissionDisabled = problem.status !== 'PUBLISHED' || isSubmitting;

    const leftPanelTabs = [
        {
            label: 'Description',
            content: <ProblemDetails problem={problem} />,
        },
        {
            label: 'Submissions',
            content: <SubmissionsList submissions={submissions} onSubmissionClick={handleSubmissionClick} />,
        }
    ];

    return (
        <>
            <div className="flex h-[calc(100vh-4rem)]">
                <div className="w-1/2 border-r border-gray-200 dark:border-gray-700">
                    <Tabs tabs={leftPanelTabs} activeTab={activeTab} setActiveTab={setActiveTab} />
                </div>

                <div className="w-1/2">
                    <CodeEditor 
                        language={language} 
                        setLanguage={setLanguage}
                        code={code}
                        setCode={setCode}
                        onSubmit={handleSubmit}
                        isSubmittingDisabled={isSubmissionDisabled}
                        timer={timer}
                    />
                </div>
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