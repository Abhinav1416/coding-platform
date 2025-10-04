import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useProblem } from '../hooks/useProblem';
import { useTimer } from '../hooks/useTimer';
import ProblemDetails from '../components/ProblemDetails';
import CodeEditor from '../components/CodeEditor';


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

    const handleSubmit = () => {
        if (!problem) return;
        console.log('Submitting solution...');
        console.log({
            problemId: problem.id,
            language,
            code,
            matchId: null
        });
        alert('Submission logic not implemented yet.');
    };

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (error) {
        return <div className="text-red-500 text-center p-8">{error}</div>;
    }

    if (!problem) {
        return <div className="text-center p-8">Problem not found.</div>;
    }

    const isSubmissionDisabled = problem.status !== 'PUBLISHED';

    return (
        <div className="flex h-[calc(100vh-4rem)]">
            <div className="w-1/2">
                <ProblemDetails problem={problem} />
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
    );
};

export default ProblemPage;