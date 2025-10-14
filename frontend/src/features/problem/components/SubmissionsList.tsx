import React from 'react';
import type { SubmissionSummary } from '../types/problem';


interface SubmissionsListProps {
    submissions: SubmissionSummary[];
    onSubmissionClick: (submissionId: string) => void;
}

export const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
    const statusStyles: { [key: string]: string } = {
        ACCEPTED: 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
        WRONG_ANSWER: 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200',
        PENDING: 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200 animate-pulse',
        PROCESSING: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 animate-pulse',
        COMPILATION_ERROR: 'bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-200',
        RUNTIME_ERROR: 'bg-orange-100 dark:bg-orange-900 text-orange-800 dark:text-orange-200',
        TIME_LIMIT_EXCEEDED: 'bg-purple-100 dark:bg-purple-900 text-purple-800 dark:text-purple-200',
        INTERNAL_ERROR: 'bg-pink-100 dark:bg-pink-900 text-pink-800 dark:text-pink-200',
    };
    
    const style = statusStyles[status] || 'bg-gray-200 text-gray-900';
    const formattedStatus = status.replace(/_/g, ' ');

    return <span className={`px-2.5 py-1 text-xs font-semibold rounded-full ${style}`}>{formattedStatus}</span>;
};


const SubmissionsList: React.FC<SubmissionsListProps> = ({ submissions, onSubmissionClick }) => {
    if (submissions.length === 0) {
        return <div className="p-4 text-center text-gray-500 dark:text-gray-400">No submissions yet.</div>;
    }

    return (
        <div className="p-4 h-full overflow-y-auto">
            <h3 className="text-lg font-semibold mb-4 text-gray-800 dark:text-gray-200">My Submissions</h3>
            <div className="space-y-3">
                {submissions.map((sub) => (
                    <button 
                        key={sub.id} 
                        onClick={() => onSubmissionClick(sub.id)}
                        className="w-full p-3 bg-white dark:bg-gray-800 rounded-lg shadow-sm flex justify-between items-center text-left hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                    >
                        <div className="flex flex-col">
                            <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                                {new Date(sub.createdAt).toLocaleString()}
                            </span>
                            <span className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                                {sub.language}
                            </span>
                        </div>
                        <StatusBadge status={sub.status} />
                    </button>
                ))}
            </div>
        </div>
    );
};

export default SubmissionsList;