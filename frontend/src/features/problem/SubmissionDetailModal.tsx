import React from 'react';
import type { SubmissionDetails } from './types/problem';
import { StatusBadge } from './components/SubmissionsList';

const DetailRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
    <div className="grid grid-cols-3 gap-4 py-2">
        <dt className="text-sm font-medium text-gray-500 dark:text-gray-400">{label}</dt>
        <dd className="text-sm text-gray-900 dark:text-gray-100 col-span-2">{value}</dd>
    </div>
);

const CodeBlock: React.FC<{ title: string; content: string | null }> = ({ title, content }) => {
    if (!content) return null;
    return (
        <div className="mt-4">
            <h4 className="text-md font-semibold text-gray-800 dark:text-gray-200">{title}</h4>
            <pre className="mt-2 bg-gray-100 dark:bg-gray-900 p-4 rounded-md text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap font-mono">
                <code>{content}</code>
            </pre>
        </div>
    );
};

const SubmissionDetailModal: React.FC<{ submission: SubmissionDetails | null; onClose: () => void; }> = ({ submission, onClose }) => {
    if (!submission) return null;

    const renderResultDetails = () => {
        switch (submission.status) {
            case 'ACCEPTED':
                return (
                    <>
                        <DetailRow label="Runtime" value={`${submission.runtimeMs} ms`} />
                        <DetailRow label="Memory" value={`${submission.memoryKb} KB`} />
                    </>
                );
            case 'COMPILATION_ERROR':
            case 'RUNTIME_ERROR':
            case 'INTERNAL_ERROR':
                return <CodeBlock title="Error Details" content={submission.stderr} />;
            case 'WRONG_ANSWER':
                return <CodeBlock title="Your Output (stdout)" content={submission.stdout} />;
            default:
                return null;
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4" onClick={onClose}>
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-xl w-full max-w-3xl" onClick={e => e.stopPropagation()}>
                <div className="flex justify-between items-center pb-4 border-b border-gray-200 dark:border-gray-700">
                    <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Submission Result</h2>
                    <button onClick={onClose} className="text-2xl text-gray-500 hover:text-gray-800 dark:hover:text-gray-200">&times;</button>
                </div>
                <div className="mt-4">
                    <dl>
                        <DetailRow label="Status" value={<StatusBadge status={submission.status} />} />
                        <DetailRow label="Problem" value={submission.problemTitle} />
                        <DetailRow label="Language" value={submission.language} />
                        <DetailRow label="Submitted At" value={new Date(submission.createdAt).toLocaleString()} />
                        {renderResultDetails()}
                    </dl>
                    <CodeBlock title="Your Code" content={submission.code} />
                </div>
            </div>
        </div>
    );
};

export default SubmissionDetailModal;