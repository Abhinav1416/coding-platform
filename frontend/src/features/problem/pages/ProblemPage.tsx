import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useProblem } from '../hooks/useProblem';
import { useTimer } from '../hooks/useTimer';
import ProblemDetails from '../components/ProblemDetails';
import CodeEditor from '../components/CodeEditor';
import SubmissionsList from '../components/SubmissionsList';

import { createSubmission, getProblemSubmissions, getSubmissionDetails } from '../services/problemService';
import * as stompClient from '../../../core/sockets/stompClient';
import type { SubmissionDetails, SubmissionSummary } from '../types/problem';
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

    // Memoize the update function to prevent re-subscriptions
    const handleRealTimeUpdate = useCallback((result: { status: string; submissionId: string }) => {
        console.log('Received result:', result);
        setSubmissions(prevSubs => 
            prevSubs.map(sub => 
                sub.id === result.submissionId ? { ...sub, status: result.status } : sub
            )
        );
    }, []);

    // Effect to fetch initial submissions and manage WebSocket connection
    useEffect(() => {
        if (problem) {
            getProblemSubmissions(problem.id).then(response => {
                setSubmissions(response.submissions);
            });
            
            stompClient.connect(() => {
                // If there are pending submissions on load, re-subscribe
                submissions.forEach(sub => {
                    if (sub.status === 'PENDING' || sub.status === 'PROCESSING') {
                        stompClient.subscribeToSubmissionResult(sub.id, handleRealTimeUpdate);
                    }
                });
            });
        }
        // Disconnect from WebSocket on component unmount
        return () => {
            stompClient.disconnect();
        };
    }, [problem, submissions, handleRealTimeUpdate]);


    const handleSubmit = async () => {
        if (!problem || isSubmitting) return;

        setIsSubmitting(true);
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
                language: language.toUpperCase(),
                createdAt: new Date().toISOString(),
            };
            setSubmissions(prev => [newSubmission, ...prev]);
            
            // Subscribe to the result topic for this new submission
            stompClient.subscribeToSubmissionResult(submissionId, handleRealTimeUpdate);

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

    return (
        <>
            <div className="flex h-[calc(100vh-4rem)]">
                {/* Left Panel: Problem Details and Submission History */}
                <div className="w-1/2 flex flex-col">
                    <div className="flex-1 overflow-y-auto">
                        <ProblemDetails problem={problem} />
                    </div>
                    <div className="flex-shrink-0 h-1/3 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
                        <SubmissionsList submissions={submissions} onSubmissionClick={handleSubmissionClick} />
                    </div>
                </div>

                {/* Right Panel: Code Editor */}
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