import React, { useState } from 'react';
import { useProblems } from '../hooks/useProblems';
import { ProblemList } from '../components/ProblemList';
import { ProblemFilters } from '../components/ProblemFilters';
import { PaginationControls } from '../components/PaginationControls';

const ProblemsPage: React.FC = () => {
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);

  // V-- The fix is here --V
  const { data, isLoading, isError, error, isPlaceholderData } = useProblems({
    page: currentPage,
    tags: selectedTags,
  });

  const handleTagChange = (tag: string) => {
    setSelectedTags(prev =>
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
    setCurrentPage(0);
  };
  
  const handleClearFilters = () => {
    setSelectedTags([]);
    setCurrentPage(0);
  };
  
  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-4xl font-bold text-white">Problems</h1>
        <p className="text-gray-400 mt-2">Challenge yourself with our curated list of problems.</p>
      </header>

      <ProblemFilters 
        selectedTags={selectedTags}
        onTagChange={handleTagChange}
        onClear={handleClearFilters}
      />

      {isLoading && <div className="text-center py-12">Loading problems...</div>}
      {isError && <div className="text-center py-12 text-red-500">Error: {(error as Error).message}</div>}
      
      {data && (
        // V-- And also here --V
        <div style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
          {data.content.length > 0 ? (
            <>
              <ProblemList problems={data.content} />
              <PaginationControls
                currentPage={data.number}
                totalPages={data.totalPages}
                onPageChange={setCurrentPage}
              />
            </>
          ) : (
             <div className="text-center py-12 text-gray-500">
                No problems found matching your criteria.
             </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ProblemsPage;