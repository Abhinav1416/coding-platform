import React from 'react';
import type { ProblemDetail } from '../types/problem';


interface ProblemDetailsProps {
    problem: ProblemDetail;
}

const ProblemDetails: React.FC<ProblemDetailsProps> = ({ problem }) => {
    const timeInSeconds = problem.timeLimitMs / 1000;
    const memoryInMegabytes = (problem.memoryLimitKb / 1000).toFixed(2);

    const formatTestCase = (data: any): string => {
        if (typeof data === 'string') {
            return data;
        }
        return JSON.stringify(data, null, 2);
    };

    return (
        <div className="p-6 overflow-y-auto h-full bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-100">
            <h1 className="text-3xl font-bold mb-4">{problem.title}</h1>

            <div className="flex items-center space-x-4 mb-6 text-sm text-gray-600 dark:text-gray-400">
                <span>Time Limit: {timeInSeconds} s</span>
                <span>Memory Limit: {memoryInMegabytes} MB</span>
                <span>Points: {problem.points}</span>
            </div>
            <div className="prose dark:prose-invert max-w-none">
                <h2 className="text-xl font-semibold mt-6 mb-2">Problem Statement</h2>
                <p dangerouslySetInnerHTML={{ __html: problem.description }}></p>

                <h2 className="text-xl font-semibold mt-6 mb-2">Constraints</h2>
                <div>
                    {problem.constraints.split('•').filter(c => c.trim()).map((constraint, index) => (
                        <p key={index} className="m-0">
                            • {constraint.trim()}
                        </p>
                    ))}
                </div>

                {problem.sampleTestCases?.map((testCase, index) => (
                    <div key={index} className="mt-6">
                        <h3 className="text-lg font-semibold mb-2">Sample Case {index + 1}</h3>
                        <div className="space-y-4">
                            <div>
                                <h4 className="font-medium">Input:</h4>
                                <pre className="bg-gray-100 dark:bg-gray-700 p-3 rounded-md">
                                    <code>{formatTestCase(testCase.stdin)}</code>
                                </pre>
                            </div>
                            <div>
                                <h4 className="font-medium">Output:</h4>
                                <pre className="bg-gray-100 dark:bg-gray-700 p-3 rounded-md">
                                    <code>{formatTestCase(testCase.expected_output)}</code>
                                </pre>
                            </div>
                            {testCase.explanation && (
                                <div>
                                    <h4 className="font-medium">Explanation:</h4>
                                    <p className="italic text-gray-600 dark:text-gray-400">{testCase.explanation}</p>
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default ProblemDetails;