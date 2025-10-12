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

/**
 * A modern, theme-aware Tabs component.
 * It features a prominent active state and subtle hover effects for a better user experience.
 */
const Tabs: React.FC<TabsProps> = ({ tabs, activeTab, setActiveTab }) => {
    return (
        <div className="flex flex-col h-full w-full">

            {/* Tab Navigation Bar */}
            <div className="flex-shrink-0 border-b border-gray-200 dark:border-zinc-700">
                <nav className="flex items-center gap-2 px-4" aria-label="Tabs">
                    {tabs.map((tab, index) => {
                        const isActive = index === activeTab;
                        
                        // Define styles for better readability
                        const baseClasses = "whitespace-nowrap py-3 px-4 font-semibold text-sm rounded-t-lg transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-[#F97316]";
                        const activeClasses = "text-[#F97316] border-b-2 border-[#F97316]";
                        const inactiveClasses = "border-b-2 border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-zinc-800";
                        
                        return (
                            <button
                                key={tab.label}
                                onClick={() => setActiveTab(index)}
                                className={`${baseClasses} ${isActive ? activeClasses : inactiveClasses}`}
                                role="tab"
                                aria-selected={isActive}
                            >
                                {tab.label}
                            </button>
                        );
                    })}
                </nav>
            </div>

            {/* Tab Content Panel */}
            <div className="flex-grow overflow-y-auto p-4 md:p-6 bg-white dark:bg-zinc-900/50">
                {tabs[activeTab].content}
            </div>
        </div>
    );
};

export default Tabs;