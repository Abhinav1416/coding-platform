import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Loader2, ExternalLink, CheckCircle2, XCircle, Clock, 
  AlertTriangle, ChevronDown, ChevronUp, WifiOff, Database, Zap, 
  History, HardDrive
} from 'lucide-react';
import MainLayout from '../../../components/layout/MainLayout';
import ErrorState from '../../../components/common/ErrorState'; 
import { duelService } from '../api/duelService';
import { useDuelWebSocket } from '../hooks/useDuelWebSocket';
import { useServerTimer } from '../../../core/hooks/useServerTimer'; 
import type { DuelState, ProblemStats, SubmissionData } from '../types';


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


const BattleRow = ({ 
  problemId, link, p1Stats, p2Stats, p1Handle, p2Handle
}: { 
  problemId: string, link: string, p1Stats?: ProblemStats, p2Stats?: ProblemStats, p1Handle: string, p2Handle: string
}) => {
  const [expanded, setExpanded] = useState(false);

  const renderHistory = (stats?: ProblemStats) => {
    if (!stats || !stats.history || Object.keys(stats.history).length === 0) return <p className="text-xs text-gray-400 italic py-2">No submissions yet.</p>;
    
    return Object.entries(stats.history)
      .sort(([, a], [, b]) => b.submissionTimeSeconds - a.submissionTimeSeconds)
      .map(([key, sub]: [string, SubmissionData]) => {
        const m = Math.floor(sub.submissionTimeSeconds / 60).toString().padStart(2, '0');
        const s = (sub.submissionTimeSeconds % 60).toString().padStart(2, '0');
        
        return (
          <div key={key} className="flex justify-between items-center text-xs py-2 border-b border-gray-100 dark:border-zinc-800 last:border-0 group/row hover:bg-white dark:hover:bg-zinc-800/50 px-2 rounded transition-colors">
            <div className="flex items-center gap-3">
               <span className="text-gray-400 font-mono text-[10px] w-10">{m}:{s}</span>
               <span className={`font-mono font-bold ${sub.verdict === 'OK' ? 'text-green-600' : 'text-red-500'}`}>
                  {sub.verdict === 'OK' ? 'AC' : sub.verdict}
               </span>
            </div>
            <div className="flex items-center gap-3 text-gray-400 font-mono text-[10px]">
               {sub.timeConsumedMillis !== undefined && (
                 <span className="flex items-center gap-1" title="Execution Time">
                   <Clock size={10} /> {sub.timeConsumedMillis}ms
                 </span>
               )}
               {sub.memoryConsumedBytes !== undefined && (
                 <span className="flex items-center gap-1" title="Memory Used">
                   <HardDrive size={10} /> {Math.round(sub.memoryConsumedBytes / 1024)}KB
                 </span>
               )}
            </div>
          </div>
        );
      });
  };

  return (
    <div className={`bg-white dark:bg-zinc-900 border rounded-xl mb-4 overflow-hidden transition-all duration-200 shadow-sm ${expanded ? 'ring-2 ring-[#F97316] border-transparent' : 'border-gray-200 dark:border-zinc-800 hover:border-[#F97316]/50'}`}>
      
      <div 
        onClick={() => setExpanded(!expanded)}
        className="flex flex-col md:grid md:grid-cols-12 items-center p-4 cursor-pointer gap-4 md:gap-0 group"
      >    
        <div className="w-full md:col-span-3 flex md:flex-col justify-between md:justify-center items-center md:border-r border-gray-100 dark:border-zinc-800 px-2">
          <span className="md:hidden text-xs font-bold text-gray-500">{p1Handle}</span>
          <VerdictBadge status={p1Stats?.status || 'NONE'} time={p1Stats?.bestTime} />
        </div>

        <div className="w-full md:col-span-6 flex flex-col items-center justify-center px-2 text-center relative">
          <h3 className="text-lg font-bold text-gray-900 dark:text-white group-hover:text-[#F97316] transition-colors">
            Problem {problemId}
          </h3>
          <a 
            href={link} 
            target="_blank" 
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()} 
            className="flex items-center gap-1 text-xs text-blue-500 hover:text-blue-400 hover:underline font-medium mt-1"
          >
            Open in Codeforces <ExternalLink size={10} />
          </a>
        </div>

        <div className="w-full md:col-span-3 flex md:flex-col justify-between md:justify-center items-center md:border-l border-gray-100 dark:border-zinc-800 px-2">
          <span className="md:hidden text-xs font-bold text-gray-500">{p2Handle}</span>
          <VerdictBadge status={p2Stats?.status || 'NONE'} time={p2Stats?.bestTime} />
        </div>
      </div>

      <div 
        onClick={() => setExpanded(!expanded)}
        className="bg-gray-50 dark:bg-zinc-800/50 py-1 flex justify-center cursor-pointer hover:bg-gray-100 dark:hover:bg-zinc-800 transition-colors border-t border-gray-100 dark:border-zinc-800"
      >
        {expanded ? (
           <ChevronUp size={16} className="text-gray-400" />
        ) : (
           <div className="flex items-center gap-1 text-xs text-gray-400 uppercase tracking-widest font-bold">
              <History size={12} /> View Details
              <ChevronDown size={14} />
           </div>
        )}
      </div>

      {expanded && (
        <div className="grid grid-cols-1 md:grid-cols-2 bg-gray-50 dark:bg-zinc-950/50 border-t border-gray-200 dark:border-zinc-800">
          <div className="p-4 border-b md:border-b-0 md:border-r border-gray-200 dark:border-zinc-800">
            <div className="flex justify-between items-center mb-3">
               <p className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-2">
                 <span className="w-2 h-2 rounded-full bg-gray-400"></span> {p1Handle}
               </p>
               {p1Stats?.penalty !== undefined && p1Stats.penalty > 0 && (
                 <span className="text-[10px] text-red-500 font-mono font-bold">+{p1Stats.penalty} Pen</span>
               )}
            </div>
            {renderHistory(p1Stats)}
          </div>
          
          <div className="p-4">
            <div className="flex justify-between items-center mb-3">
               <p className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-2">
                 <span className="w-2 h-2 rounded-full bg-gray-400"></span> {p2Handle}
               </p>
               {p2Stats?.penalty !== undefined && p2Stats.penalty > 0 && (
                 <span className="text-[10px] text-red-500 font-mono font-bold">+{p2Stats.penalty} Pen</span>
               )}
            </div>
            {renderHistory(p2Stats)}
          </div>
        </div>
      )}
    </div>
  );
};


const DuelArenaPage = () => {
  const { duelId } = useParams<{ duelId: string }>();
  const navigate = useNavigate();
  
  const { gameState: liveState, isConnected } = useDuelWebSocket(duelId);
  const [initialState, setInitialState] = useState<DuelState | null>(null);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    if (!duelId) return;
    duelService.getDuelState(duelId)
      .then(setInitialState)
      .catch(() => setIsError(true));
  }, [duelId]);

  const gameState = liveState || initialState;

  useEffect(() => {
    if (gameState?.status === 'FINISHED') {
      navigate(`/duel/result/${duelId}`);
    }
  }, [gameState?.status, duelId, navigate]);

  const { timeLeft, isEnded } = useServerTimer(gameState?.startTime, gameState?.durationMinutes);

  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = ''; 
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, []);

  if (isError) return <MainLayout><ErrorState onRetry={() => window.location.reload()} /></MainLayout>;
  if (!gameState) return <MainLayout><div className="flex justify-center items-center h-[60vh]"><Loader2 className="animate-spin text-[#F97316]" size={48} /></div></MainLayout>;

  const p1 = gameState.player1Handle;
  const p2 = gameState.player2Handle || "Opponent";
  
  const p1Data = gameState.scoreboard?.users?.[p1];
  const p2Data = gameState.scoreboard?.users?.[p2];

  const p1Score = p1Data?.solved || 0;
  const p2Score = p2Data?.solved || 0;
  
  const p1Penalty = p1Data?.penalty || 0;
  const p2Penalty = p2Data?.penalty || 0;

  return (
    <MainLayout>
      {!isConnected && (
         <div className="fixed top-[70px] left-0 right-0 z-40 bg-red-500 text-white text-center py-2 text-sm font-bold animate-pulse flex items-center justify-center gap-2 shadow-md">
           <WifiOff size={16} /> Connection lost. Attempting to reconnect...
         </div>
      )}

      {isEnded && gameState.status === 'LIVE' && (
         <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center">
            <div className="bg-white dark:bg-zinc-900 p-8 rounded-2xl shadow-2xl text-center border border-gray-200 dark:border-zinc-700 max-w-sm mx-4">
               <Loader2 className="w-12 h-12 text-[#F97316] animate-spin mx-auto mb-4" />
               <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Time's Up!</h2>
               <p className="text-gray-500 dark:text-gray-400 mt-2">Syncing final submissions...</p>
            </div>
         </div>
      )}

      <div className="container mx-auto max-w-4xl pt-8 px-4 pb-20">
        
        <div className="grid grid-cols-3 items-center mb-8 bg-white dark:bg-zinc-900 p-6 rounded-2xl shadow-sm border border-gray-200 dark:border-zinc-800">

          <div className="text-center">
            <h2 className="text-lg md:text-xl font-bold text-gray-900 dark:text-white truncate px-2">{p1}</h2>
            <div className="flex flex-col items-center">
              <p className="text-3xl md:text-4xl font-bold text-[#F97316] mt-1">{p1Score}</p>
              <p className="text-xs font-mono text-gray-500 mt-1">Penalty: {p1Penalty}</p>
            </div>
          </div>

          <div className="flex flex-col items-center">
             <div className="bg-gray-100 dark:bg-zinc-800 px-4 md:px-6 py-2 rounded-full mb-2">
                <span className="font-mono text-xl md:text-2xl font-bold text-gray-800 dark:text-gray-200">{timeLeft}</span>
             </div>
             <span className="text-[10px] md:text-xs text-red-500 font-bold animate-pulse uppercase tracking-wider flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-red-500 inline-block"></span> Live
             </span>
          </div>

          <div className="text-center">
            <h2 className="text-lg md:text-xl font-bold text-gray-900 dark:text-white truncate px-2">{p2}</h2>
            <div className="flex flex-col items-center">
              <p className="text-3xl md:text-4xl font-bold text-blue-500 mt-1">{p2Score}</p>
              <p className="text-xs font-mono text-gray-500 mt-1">Penalty: {p2Penalty}</p>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="hidden md:grid grid-cols-12 text-xs font-bold text-gray-400 uppercase tracking-wider px-4 mb-2 text-center">
            <div className="col-span-3">{p1} Status</div>
            <div className="col-span-6">Problem</div>
            <div className="col-span-3">{p2} Status</div>
          </div>

          {gameState.problemLinks.map((link: string, idx: number) => {
             const pid = gameState.problemIds[idx];
             const p1Stats = gameState.scoreboard?.users?.[p1]?.problems?.[pid];
             const p2Stats = gameState.scoreboard?.users?.[p2]?.problems?.[pid];

             return (
               <BattleRow 
                 key={idx}
                 problemId={pid || `P${idx+1}`}
                 link={link}
                 p1Stats={p1Stats}
                 p2Stats={p2Stats}
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

export default DuelArenaPage;