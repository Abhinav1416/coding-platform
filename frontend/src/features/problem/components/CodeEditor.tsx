import React from 'react';
import Editor from '@monaco-editor/react';
import { useTheme } from '../../../core/context/ThemeContext';
import Timer from './Timer';

type Language = 'cpp' | 'java' | 'python';

interface CodeEditorProps {
    language: Language;
    setLanguage: (language: Language) => void;
    code: string;
    setCode: (code: string) => void;
    onSubmit: () => void;
    isSubmittingDisabled: boolean;
    timer: {
        time: number;
        isActive: boolean;
        handleStart: () => void;
        handlePause: () => void;
        handleReset: () => void;
    };
}

const languageMap: { [key in Language]: string } = {
    cpp: 'cpp',
    java: 'java',
    python: 'python',
};

const CodeEditor: React.FC<CodeEditorProps> = ({ language, setLanguage, code, setCode, onSubmit, isSubmittingDisabled, timer }) => {
    const { theme } = useTheme();

    return (
        <div className="flex flex-col h-full bg-white dark:bg-gray-900">
            <div className="flex items-center justify-between p-2 bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
                <select
                    value={language}
                    onChange={(e) => setLanguage(e.target.value as Language)}
                    className="p-2 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 border border-gray-300 dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    <option value="cpp">C++</option>
                    <option value="java">Java</option>
                    <option value="python">Python</option>
                </select>
                
                <div className="flex items-center gap-4">
                    <Timer 
                        time={timer.time}
                        isActive={timer.isActive}
                        onStart={timer.handleStart}
                        onPause={timer.handlePause}
                        onReset={timer.handleReset}
                    />

                    <button 
                        onClick={() => alert('Run Code feature is not yet implemented!')}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-200 font-semibold rounded-md hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors"
                        title="Run Code"
                    >
                
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                            <path d="M6.3 2.841A1.5 1.5 0 0 0 4 4.11v11.78a1.5 1.5 0 0 0 2.3 1.269l9.344-5.89a1.5 1.5 0 0 0 0-2.538L6.3 2.841Z" />
                        </svg>
                        Run
                    </button>
                    
                    <button 
                        onClick={onSubmit}
                        disabled={isSubmittingDisabled}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white font-semibold rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:bg-gray-500 disabled:cursor-not-allowed transition-colors"
                        title={isSubmittingDisabled ? 'Submissions Disabled' : 'Submit Solution'}
                    >

                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                          <path d="M9.309 3.14c-.094-.323-.527-.323-.62 0L4.363 8.32c-.2.685.028 1.488.544 1.838l3.187 2.126a2.25 2.25 0 0 0 2.373 0l3.187-2.126c.516-.35.744-1.153.544-1.838L9.309 3.14Z" />
                          <path fillRule="evenodd" d="M11.664 12.84a2.25 2.25 0 0 1-3.328 0 2.25 2.25 0 0 0-2.373 0l-3.187 2.126c-.516.35-.744 1.153-.544 1.838L4.363 17.68c.094.323.527.323.62 0l4.326-5.18a2.25 2.25 0 0 1 2.373 0l4.326 5.18c.094.323.527.323.62 0l1.414-4.949c.2-.685-.028-1.488-.544-1.838L11.664 12.84Z" clipRule="evenodd" />
                        </svg>
                        Submit
                    </button>
                </div>
            </div>

            <div className="flex-grow">
                <Editor
                    height="100%"
                    language={languageMap[language]}
                    value={code}
                    onChange={(value) => setCode(value || '')}
                    theme={theme === 'dark' ? 'vs-dark' : 'light'}
                    options={{
                        minimap: { enabled: false },
                        fontSize: 14,
                        wordWrap: 'on',
                    }}
                />
            </div>
        </div>
    );
};

export default CodeEditor;