import React from 'react';
import type { Problem } from '../types/problem';
import { Link } from 'react-router-dom';
import { FaCheckCircle } from 'react-icons/fa';

interface ProblemListProps {
  problems: Problem[];
}

// Helper to determine difficulty color and text
const getDifficultyInfo = (difficulty: number) => {
  if (difficulty < 1200) return { text: 'Easy', color: 'text-green-400' };
  if (difficulty < 1600) return { text: 'Medium', color: 'text-yellow-400' };
  return { text: 'Hard', color: 'text-red-400' };
};

export const ProblemList: React.FC<ProblemListProps> = ({ problems }) => {
  return (
    <div className="bg-zinc-900 rounded-lg border border-white/10">
      <div className="divide-y divide-zinc-800">
        {/* Table Header */}
        <div className="px-6 py-4 grid grid-cols-12 gap-4 text-sm font-semibold text-gray-400">
          <div className="col-span-1">Status</div>
          <div className="col-span-6">Title</div>
          <div className="col-span-2">Difficulty</div>
          <div className="col-span-3">Tags</div>
        </div>
        
        {/* Table Body */}
        {problems.map((problem) => {
          const { text, color } = getDifficultyInfo(problem.difficulty);
          return (
            <div
              key={problem.problemId}
              className="px-6 py-4 grid grid-cols-12 gap-4 items-center hover:bg-zinc-800/50 transition-colors"
            >
              {/* Status (Mocked for now) */}
              <div className="col-span-1 text-center">
                 <FaCheckCircle className="text-green-500 opacity-20 text-lg" title="Solved" />
              </div>

              {/* Title */}
              <div className="col-span-6">
                <Link to={`/problems/${problem.slug}`} className="hover:text-[#F97316] transition-colors">
                  {problem.title}
                </Link>
              </div>

              {/* Difficulty */}
              <div className={`col-span-2 font-medium ${color}`}>{text}</div>

              {/* Tags */}
              <div className="col-span-3 flex flex-wrap gap-1.5">
                {problem.tags.slice(0, 2).map(tag => (
                   <span key={tag} className="bg-zinc-700 text-xs text-gray-300 px-2 py-0.5 rounded">
                     {tag}
                   </span>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};