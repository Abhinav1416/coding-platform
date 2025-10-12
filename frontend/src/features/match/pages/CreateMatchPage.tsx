import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, Copy, Check } from 'lucide-react';
import { useCreateMatch } from '../hooks/useCreateMatch';
import type { CreateMatchRequest, CreateMatchResponse, MatchEvent } from '../types/match';
import { stompService } from '../../../core/sockets/stompClient';
import MainLayout from '../../../components/layout/MainLayout';


// --- Helper function to safely get an error message ---
const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
};

// --- Main Component ---
const CreateMatchPage = () => {
  const { createMatchMutation, isLoading, error, createdMatchData } = useCreateMatch();
  const [formData, setFormData] = useState<CreateMatchRequest>({
    difficultyMin: 1200,
    difficultyMax: 1600,
    startDelayInMinutes: 1,
    durationInMinutes: 1,
  });
  const [formError, setFormError] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    // Handle empty input gracefully by not setting it to 0 immediately
    setFormData(prevState => ({ ...prevState, [name]: value === '' ? '' : parseInt(value, 10) }));
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setFormError(null);
    if (formData.difficultyMin >= formData.difficultyMax) {
      setFormError('Max difficulty must be greater than min difficulty.');
      return;
    }
    // Ensure values are numbers before mutation
    const numericFormData = {
      ...formData,
      difficultyMin: Number(formData.difficultyMin),
      difficultyMax: Number(formData.difficultyMax),
      startDelayInMinutes: Number(formData.startDelayInMinutes),
      durationInMinutes: Number(formData.durationInMinutes),
    };
    createMatchMutation(numericFormData);
  };

  // --- Theme-aware Styles ---
  const labelStyles = "block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2";
  const inputStyles = "w-full p-2 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-gray-200 rounded-md outline-none transition-colors focus:border-[#F97316] focus:ring-1 focus:ring-[#F97316]";

  return (
    <MainLayout>
      <div className="flex items-center justify-center">
        <div className="w-full max-w-md bg-white dark:bg-zinc-900 p-8 rounded-xl border border-gray-200 dark:border-white/10 shadow-lg">
          {createdMatchData ? (
            <WaitingForOpponent data={createdMatchData} />
          ) : (
            <>
              <h2 className="text-3xl font-bold text-center text-gray-900 dark:text-white mb-8">Create a New Match</h2>
              <form onSubmit={handleSubmit} className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="difficultyMin" className={labelStyles}>Min Difficulty</label>
                    <input type="number" id="difficultyMin" name="difficultyMin" value={formData.difficultyMin} onChange={handleChange} min="1000" max="3500" step="100" required className={inputStyles} />
                  </div>
                  <div>
                    <label htmlFor="difficultyMax" className={labelStyles}>Max Difficulty</label>
                    <input type="number" id="difficultyMax" name="difficultyMax" value={formData.difficultyMax} onChange={handleChange} min="1000" max="3500" step="100" required className={inputStyles} />
                  </div>
                </div>
                {formError && <p className="text-red-500 text-xs -mt-4 text-center">{formError}</p>}
                <div>
                  <label htmlFor="startDelayInMinutes" className={labelStyles}>Start Delay (minutes)</label>
                  <input type="number" id="startDelayInMinutes" name="startDelayInMinutes" value={formData.startDelayInMinutes} onChange={handleChange} min="1" max="60" required className={inputStyles} />
                </div>
                <div>
                  <label htmlFor="durationInMinutes" className={labelStyles}>Match Duration (minutes)</label>
                  <input type="number" id="durationInMinutes" name="durationInMinutes" value={formData.durationInMinutes} onChange={handleChange} min="1" max="45" required className={inputStyles} />
                </div>
                <div>
                  <button type="submit" disabled={isLoading} className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none">
                    {isLoading ? <><Loader2 className="mr-2 h-5 w-5 animate-spin" />Creating Match...</> : 'Create Match'}
                  </button>
                </div>
                {error && <p className="text-red-500 text-sm text-center mt-4">{getErrorMessage(error)}</p>}
              </form>
            </>
          )}
        </div>
      </div>
    </MainLayout>
  );
};

// --- Waiting for Opponent Component ---
const WaitingForOpponent = ({ data }: { data: CreateMatchResponse }) => {
  const navigate = useNavigate();
  const [copiedItem, setCopiedItem] = useState<'link' | 'code' | null>(null);

  useEffect(() => {
    if (!data.matchId) return;

    const subscription = stompService.subscribeToMatchUpdates(data.matchId, (event: MatchEvent) => {
      if (event.eventType === 'PLAYER_JOINED') {
        navigate(`/match/lobby/${data.matchId}`);
      }
    });

    return () => {
      subscription?.unsubscribe();
    };
  }, [data.matchId, navigate]);

  const handleCopy = (text: string, type: 'link' | 'code') => {
    navigator.clipboard.writeText(text);
    setCopiedItem(type);
    setTimeout(() => setCopiedItem(null), 2000);
  };

  // --- Theme-aware Styles ---
  const inputGroupStyles = "flex items-center bg-gray-100 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 rounded-md";
  const textStyles = "flex-grow p-2 font-mono text-gray-800 dark:text-gray-300 overflow-x-auto whitespace-nowrap";
  const buttonStyles = "p-2 text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors";

  return (
    <>
      <div className="text-center">
        <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Match Ready!</h2>
        <p className="text-gray-600 dark:text-gray-400 mb-8">Share the code or link with your opponent.</p>
      </div>
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Room Code</label>
          <div className={inputGroupStyles}>
            <p className={textStyles}>{data.roomCode}</p>
            <button onClick={() => handleCopy(data.roomCode, 'code')} className={buttonStyles}>
              {copiedItem === 'code' ? <Check size={18} className="text-green-500" /> : <Copy size={18} />}
            </button>
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Shareable Link</label>
          <div className={inputGroupStyles}>
            <p className={textStyles}>{data.shareableLink}</p>
            <button onClick={() => handleCopy(data.shareableLink, 'link')} className={buttonStyles}>
              {copiedItem === 'link' ? <Check size={18} className="text-green-500" /> : <Copy size={18} />}
            </button>
          </div>
        </div>
      </div>
      <div className="mt-8 text-center flex items-center justify-center gap-3 text-lg text-[#F97316]">
        <Loader2 className="animate-spin" />
        <span>Waiting for opponent to join...</span>
      </div>
    </>
  );
};

export default CreateMatchPage;
