import React from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';

interface PaginationProps {
  currentPage: number; // 0-indexed
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * A modern, theme-aware pagination component.
 * It displays page numbers and ellipses for easy navigation through larger sets of pages.
 */
export const Pagination: React.FC<PaginationProps> = ({ currentPage, totalPages, onPageChange }) => {
  if (totalPages <= 1) {
    return null;
  }

  // --- Logic to generate the page numbers to display ---
  const getPaginationItems = () => {
    const items: (number | string)[] = [];
    const pageRangeDisplayed = 2; // How many pages to show on each side of the current page
    const totalSlots = (pageRangeDisplayed * 2) + 5; // e.g., 1 ... 4 5 6 ... 10

    if (totalPages <= totalSlots) {
      // If total pages are few, show all of them
      for (let i = 1; i <= totalPages; i++) {
        items.push(i);
      }
    } else {
      // Logic for displaying ellipses
      const startPages = [1, 2];
      const endPages = [totalPages - 1, totalPages];
      const middlePages: number[] = [];

      for (let i = -pageRangeDisplayed; i <= pageRangeDisplayed; i++) {
        const page = currentPage + 1 + i;
        if (page > 0 && page <= totalPages) {
          middlePages.push(page);
        }
      }

      const allPages = new Set([...startPages, ...middlePages, ...endPages]);
      const sortedPages = Array.from(allPages).sort((a, b) => a - b);

      let lastPage = 0;
      for (const page of sortedPages) {
        if (page - lastPage > 1) {
          items.push('...');
        }
        items.push(page);
        lastPage = page;
      }
    }
    return items;
  };

  const paginationItems = getPaginationItems();

  // --- Style Definitions ---
  const baseButtonStyles = "flex items-center justify-center h-10 w-10 rounded-lg text-sm font-semibold transition-colors duration-200";
  const arrowButtonStyles = "bg-gray-100 dark:bg-zinc-800 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed";
  const pageButtonStyles = "bg-transparent text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-zinc-800";
  const activePageStyles = "!bg-[#F97316] text-white hover:!bg-[#EA580C]";
  const ellipsisStyles = "flex items-center justify-center h-10 w-10 text-gray-500 dark:text-gray-400";

  return (
    <nav className="flex justify-center items-center gap-2 mt-10">
      {/* Previous Button */}
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className={`${baseButtonStyles} ${arrowButtonStyles}`}
        aria-label="Go to previous page"
      >
        <FaChevronLeft size={14} />
      </button>

      {/* Page Number Buttons */}
      {paginationItems.map((item, index) =>
        typeof item === 'number' ? (
          <button
            key={`page-${item}`}
            onClick={() => onPageChange(item - 1)}
            className={`${baseButtonStyles} ${item - 1 === currentPage ? activePageStyles : pageButtonStyles}`}
            aria-current={item - 1 === currentPage ? 'page' : undefined}
          >
            {item}
          </button>
        ) : (
          <span key={`ellipsis-${index}`} className={ellipsisStyles}>
            ...
          </span>
        )
      )}

      {/* Next Button */}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        className={`${baseButtonStyles} ${arrowButtonStyles}`}
        aria-label="Go to next page"
      >
        <FaChevronRight size={14} />
      </button>
    </nav>
  );
};