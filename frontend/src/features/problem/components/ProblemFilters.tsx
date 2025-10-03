import React from 'react';
import { AVAILABLE_TAGS } from '../constants/tags';
import { FaTimes } from 'react-icons/fa';

interface ProblemFiltersProps {
  selectedTags: string[];
  onTagChange: (tag: string) => void;
  onClear: () => void;
}

export const ProblemFilters: React.FC<ProblemFiltersProps> = ({ selectedTags, onTagChange, onClear }) => {
  return (
    <div className="mb-6 p-4 bg-zinc-900 rounded-lg border border-white/10">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-white">Filter by Tags</h3>
        {selectedTags.length > 0 && (
          <button onClick={onClear} className="text-sm text-gray-400 hover:text-white flex items-center gap-1">
             <FaTimes /> Clear
          </button>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        {AVAILABLE_TAGS.map(tag => {
          const isSelected = selectedTags.includes(tag);
          return (
            <button
              key={tag}
              onClick={() => onTagChange(tag)}
              className={`px-3 py-1 text-sm rounded-full transition-colors ${
                isSelected
                  ? 'bg-[#F97316] text-white font-semibold'
                  : 'bg-zinc-700 hover:bg-zinc-600 text-gray-300'
              }`}
            >
              {tag}
            </button>
          );
        })}
      </div>
    </div>
  );
};