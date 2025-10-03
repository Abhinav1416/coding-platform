import React from 'react';

interface PaginationControlsProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export const PaginationControls: React.FC<PaginationControlsProps> = ({ currentPage, totalPages, onPageChange }) => {
  return (
    <div className="flex justify-center items-center gap-4 mt-8">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className="px-4 py-2 bg-zinc-800 text-white rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-zinc-700"
      >
        Previous
      </button>
      <span className="text-gray-300">
        Page {currentPage + 1} of {totalPages}
      </span>
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage + 1 >= totalPages}
        className="px-4 py-2 bg-zinc-800 text-white rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-zinc-700"
      >
        Next
      </button>
    </div>
  );
};