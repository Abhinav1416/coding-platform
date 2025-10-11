import React from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';

interface PaginationProps {
  currentPage: number; // 0-indexed
  totalPages: number;
  onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({ currentPage, totalPages, onPageChange }) => {
  if (totalPages <= 1) {
    return null;
  }

  const handlePrevious = () => {
    if (currentPage > 0) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (currentPage < totalPages - 1) {
      onPageChange(currentPage + 1);
    }
  };

  return (
    <div className="flex justify-center items-center gap-4 mt-8">
      <button
        onClick={handlePrevious}
        disabled={currentPage === 0}
        className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-300 bg-zinc-800 rounded-md hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <FaChevronLeft className="pointer-events-none" />
        Previous
      </button>

      <span className="text-sm text-gray-400">
        Page {currentPage + 1} of {totalPages}
      </span>

      <button
        onClick={handleNext}
        disabled={currentPage === totalPages - 1}
        className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-300 bg-zinc-800 rounded-md hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Next
        <FaChevronRight className="pointer-events-none" />
      </button>
    </div>
  );
};