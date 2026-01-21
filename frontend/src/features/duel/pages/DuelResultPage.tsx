import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Trophy, Home, Share2, Loader2, CheckCircle2, XCircle, 
  Clock, AlertTriangle, Database, Zap, ChevronDown, ChevronUp, 
  History, HardDrive, Medal, AlertCircle
} from 'lucide-react';
import MainLayout from '../../../components/layout/MainLayout';
import { duelService } from '../api/duelService';
import type { DuelState, ProblemStats, SubmissionData } from '../types';
import { toast } from 'react-hot-toast';

const VerdictBadge = ({ status, time }: { status: string, time?: number }) => {
  switch (status) {
    case 'OK':
      return (
        <div className="flex flex-col items-center text-green-600 dark:text-green-400">
          <CheckCircle2 size={28} />
          {time !== undefined && time > 0 && (
            <span className="text-xs font-bold mt-1 font-mono">{Math.floor(time / 60)}m</span>
          )}
        </div>
      );
    case 'WRONG_ANSWER': return <div className="text-red-500" title="Wrong Answer"><XCircle size={28} /></div>;
    case 'COMPILATION_ERROR': return <div className="text-yellow-500" title="Compilation Error"><AlertTriangle size={28} /></div>;
    case 'TIME_LIMIT_EXCEEDED': return <div className="text-orange-500" title="Time Limit Exceeded"><Clock size={28} /></div>;
    case 'MEMORY_LIMIT_EXCEEDED': return <div className="text-purple-500" title="Memory Limit Exceeded"><Database size={28} /></div>;
    case 'RUNTIME_ERROR': return <div className="text-pink-500" title="Runtime Error"><Zap size={28} /></div>;
    default: return <div className="text-gray-300 dark:text-zinc-700 font-mono text-2xl font-bold">-</div>;
  }
};

const ResultRow = ({ 
  problemId, link, p1Stats, p2Stats, p1Handle, p2Handle
}: { 
  problemId: string, link: string, p1Stats?: ProblemStats, p2Stats?: ProblemStats, p1Handle: string, p2Handle: string
}) => {
  const [expanded, setExpanded] = useState(false);

  const renderHistory = (stats?: ProblemStats) => {
    if (!stats || !stats.history || Object.keys(stats.history).length === 0) return <p className="text-xs text-gray-400 italic py-2">No submissions.</p>;
    
    return Object.entries(stats.history)
      .sort(([, a], [, b]) => b.submissionTimeSeconds - a.submissionTimeSeconds)
      .map(([key, sub]: [string, SubmissionData]) => {
        const m = Math.floor(sub.submissionTimeSeconds / 60).toString().padStart(2, '0');
        const s = (sub.submissionTimeSeconds % 60).toString().padStart(2, '0');
        
        return (
          <div key={key} className="flex justify-between items-center text-xs py-2 border-b border-gray-100 dark:border-zinc-800 last:border-0 hover:bg-white dark:hover:bg-zinc-800/50 px-2 rounded transition-colors">
            <div className="flex items-center gap-3">
               <span className="text-gray-400 font-mono text-[10px] w-10">{m}:{s}</span>
               <span className={`font-mono font-bold ${sub.verdict === 'OK' ? 'text-green-600' : 'text-red-500'}`}>
                  {sub.verdict === 'OK' ? 'AC' : sub.verdict}
               </span>
            </div>
            <div className="flex items-center gap-3 text-gray-400 font-mono text-[10px]">
               {sub.timeConsumedMillis !== undefined && (
                 <span className="flex items-center gap-1" title="Time"><Clock size={10} /> {sub.timeConsumedMillis}ms</span>
               )}
               {sub.memoryConsumedBytes !== undefined && (
                 <span className="flex items-center gap-1" title="Memory"><HardDrive size={10} /> {Math.round(sub.memoryConsumedBytes / 1024)}KB</span>
               )}
            </div>
          </div>
        );
      });
  };

  return (
    <div className={`bg-white dark:bg-zinc-900 border rounded-xl mb-4 overflow-hidden shadow-sm ${expanded ? 'ring-2 ring-[#F97316] border-transparent' : 'border-gray-200 dark:border-zinc-800 hover:border-[#F97316]/50'}`}>
      <div 
        onClick={() => setExpanded(!expanded)}
        className="flex flex-col md:grid md:grid-cols-12 items-center p-4 cursor-pointer gap-4 md:gap-0"
      >
        <div className="w-full md:col-span-3 flex md:flex-col justify-between md:justify-center items-center md:border-r border-gray-100 dark:border-zinc-800 px-2">
          <span className="md:hidden text-xs font-bold text-gray-500">{p1Handle}</span>
          <VerdictBadge status={p1Stats?.status || 'NONE'} time={p1Stats?.bestTime} />
        </div>
        <div className="w-full md:col-span-6 text-center px-2">
          <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-[#F97316] transition-colors">Problem {problemId}</h3>
          <a href={link} target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()} className="text-xs text-blue-500 hover:underline">View Problem</a>
        </div>
        <div className="w-full md:col-span-3 flex md:flex-col justify-between md:justify-center items-center md:border-l border-gray-100 dark:border-zinc-800 px-2">
          <span className="md:hidden text-xs font-bold text-gray-500">{p2Handle}</span>
          <VerdictBadge status={p2Stats?.status || 'NONE'} time={p2Stats?.bestTime} />
        </div>
      </div>

      <div onClick={() => setExpanded(!expanded)} className="bg-gray-50 dark:bg-zinc-800/50 py-1 flex justify-center cursor-pointer hover:bg-gray-100 dark:hover:bg-zinc-800 border-t border-gray-100 dark:border-zinc-800">
        {expanded ? <ChevronUp size={16} className="text-gray-400" /> : <div className="flex items-center gap-1 text-xs text-gray-400 uppercase font-bold"><History size={12} /> Details <ChevronDown size={14} /></div>}
      </div>

      {expanded && (
        <div className="grid grid-cols-1 md:grid-cols-2 bg-gray-50 dark:bg-zinc-950/50 border-t border-gray-200 dark:border-zinc-800">
          <div className="p-4 border-b md:border-b-0 md:border-r border-gray-200 dark:border-zinc-800">
             <div className="flex justify-between mb-2">
                <span className="text-xs font-bold text-gray-500">{p1Handle}</span>
                {p1Stats?.penalty && p1Stats.penalty > 0 ? <span className="text-[10px] text-red-500 font-bold">+{p1Stats.penalty} Pen</span> : null}
             </div>
             {renderHistory(p1Stats)}
          </div>
          <div className="p-4">
             <div className="flex justify-between mb-2">
                <span className="text-xs font-bold text-gray-500">{p2Handle}</span>
                {p2Stats?.penalty && p2Stats.penalty > 0 ? <span className="text-[10px] text-red-500 font-bold">+{p2Stats.penalty} Pen</span> : null}
             </div>
             {renderHistory(p2Stats)}
          </div>
        </div>
      )}
    </div>
  );
};

const DuelResultPage = () => {
  const { duelId } = useParams<{ duelId: string }>();
  const navigate = useNavigate();
  const [gameState, setGameState] = useState<DuelState | null>(null);

  useEffect(() => {
    if (duelId) duelService.getDuelState(duelId).then(setGameState).catch(console.error);
  }, [duelId]);

  if (!gameState) return <MainLayout><div className="flex justify-center pt-24"><Loader2 className="animate-spin text-[#F97316]" size={48} /></div></MainLayout>;

  const p1 = gameState.player1Handle;
  const p2 = gameState.player2Handle || "Opponent";
  const p1Data = gameState.scoreboard?.users?.[p1];
  const p2Data = gameState.scoreboard?.users?.[p2];
  
  const s1 = p1Data?.solved || 0;
  const s2 = p2Data?.solved || 0;
  const pen1 = p1Data?.penalty || 0;
  const pen2 = p2Data?.penalty || 0;

  let resultMessage = "It's a Draw!";
  let winnerHandle = null;

  if (s1 > s2) { resultMessage = `${p1} Wins!`; winnerHandle = p1; }
  else if (s2 > s1) { resultMessage = `${p2} Wins!`; winnerHandle = p2; }
  else if (s1 > 0) {
     if (pen1 < pen2) { resultMessage = `${p1} Wins (on Time)!`; winnerHandle = p1; }
     else if (pen2 < pen1) { resultMessage = `${p2} Wins (on Time)!`; winnerHandle = p2; }
  }

  const shareResult = () => {
    navigator.clipboard.writeText(window.location.href);
    toast.success("Link copied to clipboard!");
  };

  return (
    <MainLayout>
      <div className="container mx-auto max-w-4xl pt-10 px-4 pb-20">
        
        <div className="text-center mb-10">
           <div className="inline-flex items-center justify-center p-4 bg-orange-100 dark:bg-orange-900/30 rounded-full mb-4 ring-4 ring-orange-50 dark:ring-orange-900/10">
              <Trophy size={48} className="text-[#F97316]" />
           </div>
           <h1 className="text-4xl md:text-5xl font-extrabold text-gray-900 dark:text-white mb-2">{resultMessage}</h1>
           <p className="text-gray-500 dark:text-gray-400">Match Completed</p>
        </div>

        <div className="grid grid-cols-3 bg-white dark:bg-zinc-900 rounded-2xl shadow-lg border border-gray-200 dark:border-zinc-800 p-8 mb-8 relative overflow-hidden">
           {winnerHandle === p1 && <div className="absolute left-0 top-0 bottom-0 w-1/3 bg-orange-50/50 dark:bg-orange-900/10 -z-0"></div>}
           {winnerHandle === p2 && <div className="absolute right-0 top-0 bottom-0 w-1/3 bg-blue-50/50 dark:bg-blue-900/10 -z-0"></div>}

           <div className="text-center z-10 relative">
              <div className="text-xl font-bold text-gray-900 dark:text-white mb-2">{p1}</div>
              <div className="text-5xl font-bold text-[#F97316] mb-1">{s1}</div>
              <div className="text-xs text-gray-500 font-mono font-bold uppercase tracking-wider">Penalty: {pen1}</div>
              {winnerHandle === p1 && <div className="mt-2 inline-flex items-center gap-1 text-xs font-bold text-orange-600 bg-orange-100 px-2 py-1 rounded-full"><Medal size={12}/> Winner</div>}
           </div>

           <div className="flex flex-col items-center justify-center z-10">
              <span className="text-gray-300 dark:text-zinc-700 font-black text-2xl italic">VS</span>
           </div>

           <div className="text-center z-10 relative">
              <div className="text-xl font-bold text-gray-900 dark:text-white mb-2">{p2}</div>
              <div className="text-5xl font-bold text-blue-500 mb-1">{s2}</div>
              <div className="text-xs text-gray-500 font-mono font-bold uppercase tracking-wider">Penalty: {pen2}</div>
              {winnerHandle === p2 && <div className="mt-2 inline-flex items-center gap-1 text-xs font-bold text-blue-600 bg-blue-100 px-2 py-1 rounded-full"><Medal size={12}/> Winner</div>}
           </div>
        </div>

        <div className="flex justify-center gap-4 mb-10">
           <button onClick={() => navigate('/home')} className="flex items-center gap-2 px-6 py-3 bg-gray-100 dark:bg-zinc-800 hover:bg-gray-200 dark:hover:bg-zinc-700 rounded-lg font-bold text-gray-700 dark:text-white transition-colors">
              <Home size={18} /> Home
           </button>
           <button onClick={shareResult} className="flex items-center gap-2 px-6 py-3 bg-[#F97316] hover:bg-[#EA580C] text-white rounded-lg font-bold transition-colors shadow-lg shadow-orange-500/20">
              <Share2 size={18} /> Share Result
           </button>
        </div>

        <div className="space-y-4">
           <div className="flex items-center gap-2 mb-4 text-gray-500 dark:text-gray-400 font-bold uppercase tracking-wider text-sm">
             <AlertCircle size={16} /> Problem Breakdown
           </div>
           
           {gameState.problemLinks.map((link, idx) => {
             const pid = gameState.problemIds[idx];
             return (
               <ResultRow 
                 key={idx}
                 problemId={pid || `P${idx+1}`}
                 link={link}
                 p1Stats={gameState.scoreboard?.users?.[p1]?.problems?.[pid]}
                 p2Stats={gameState.scoreboard?.users?.[p2]?.problems?.[pid]}
                 p1Handle={p1}
                 p2Handle={p2}
               />
             );
           })}
        </div>

      </div>
    </MainLayout>
  );
};

export default DuelResultPage;