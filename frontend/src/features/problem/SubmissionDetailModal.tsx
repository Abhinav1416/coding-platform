import React from 'react';
import type { SubmissionDetails } from './types/problem';


interface SubmissionDetailModalProps {
    submission: SubmissionDetails | null;
    onClose: () => void;
}

const SubmissionDetailModal: React.FC<SubmissionDetailModalProps> = ({ submission, onClose }) => {
    if (!submission) {
        return null; // Don't render anything if no submission is selected
    }

    return (
        // Basic modal structure
        <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-xl w-full max-w-2xl">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Submission Details</h2>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-800 dark:hover:text-gray-200">&times;</button>
                </div>
                <div>
                    <p className="text-gray-800 dark:text-gray-200">Status: {submission.status}</p>
                    <p className="text-gray-800 dark:text-gray-200">Language: {submission.language}</p>
                    <pre className="bg-gray-100 dark:bg-gray-900 p-4 rounded-md mt-4 text-sm text-gray-800 dark:text-gray-200">
                        <code>
                            {submission.code}
                        </code>
                    </pre>
                    {/* More details will be added here later */}
                </div>
            </div>
        </div>
    );
};

export default SubmissionDetailModal;