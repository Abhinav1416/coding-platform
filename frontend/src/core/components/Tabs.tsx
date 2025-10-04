import React from 'react';

interface Tab {
    label: string;
    content: React.ReactNode;
}

interface TabsProps {
    tabs: Tab[];
    activeTab: number;
    setActiveTab: (index: number) => void;
}

const Tabs: React.FC<TabsProps> = ({ tabs, activeTab, setActiveTab }) => {
    return (
        <div className="flex flex-col h-full">

            <div className="flex-shrink-0 border-b border-gray-200 dark:border-gray-700">
                <nav className="flex space-x-4 px-4" aria-label="Tabs">
                    {tabs.map((tab, index) => (
                        <button
                            key={tab.label}
                            onClick={() => setActiveTab(index)}
                            className={`
                                ${index === activeTab
                                    ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-200'
                                }
                                whitespace-nowrap py-3 px-1 border-b-2 font-medium text-sm transition-colors focus:outline-none
                            `}
                        >
                            {tab.label}
                        </button>
                    ))}
                </nav>
            </div>

            <div className="flex-grow overflow-y-auto">
                {tabs[activeTab].content}
            </div>
        </div>
    );
};

export default Tabs;