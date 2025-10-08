import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, Copy, Check } from 'lucide-react';
import type { CreateMatchRequest, CreateMatchResponse, MatchEvent } from '../types/match';
import { stompService } from '../../../core/sockets/stompClient';
// Assuming you have a real useCreateMatch hook, otherwise this mock will work.
import { useCreateMatch } from '../hooks/useCreateMatch'; 

// --- Main Component ---
const CreateMatchPage = () => {
  const { createMatchMutation, isLoading, error, createdMatchData } = useCreateMatch();
  const [formData, setFormData] = useState<CreateMatchRequest>({
    difficultyMin: 1200,
    difficultyMax: 1600,
    startDelayInMinutes: 5,
    durationInMinutes: 15,
  });
  const [formError, setFormError] = useState<string | null>(null);
  
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prevState => ({ ...prevState, [name]: value === '' ? 0 : parseInt(value, 10) }));
  };
  
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setFormError(null);
    if (formData.difficultyMin >= formData.difficultyMax) {
      setFormError('Max difficulty must be greater than min difficulty.');
      return;
    }
    createMatchMutation(formData);
  };

  const labelStyles = "block text-sm font-medium text-gray-400 mb-2";
  const inputStyles = "w-full p-2 bg-zinc-800 border border-zinc-700 rounded-md outline-none transition-colors focus:border-[#F97316] focus:ring-1 focus:ring-[#F97316]";

  return (
    <div className="min-h-screen bg-gray-900 text-gray-300 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-zinc-900 p-8 rounded-xl border border-white/10 shadow-lg">
        {createdMatchData ? (
          <WaitingForOpponent data={createdMatchData} />
        ) : (
          <>
            <h2 className="text-3xl font-bold text-center text-white mb-8">Create a New Match</h2>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Form content remains the same */}
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
              {formError && <p className="text-red-400 text-xs -mt-4 text-center">{formError}</p>}
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
              {error && <p className="text-red-400 text-sm text-center mt-4">{error}</p>}
            </form>
          </>
        )}
      </div>
    </div>
  );
};

// --- Waiting for Opponent Component ---
const WaitingForOpponent = ({ data }: { data: CreateMatchResponse }) => {
  const navigate = useNavigate();
  const [copiedItem, setCopiedItem] = useState<'link' | 'code' | null>(null);

  useEffect(() => {
    // 1. Ensure the WebSocket client is activated
    stompService.connect();

    // 2. Use the specific, type-safe subscription method
    const subscription = stompService.subscribeToMatchUpdates(data.matchId, (event: MatchEvent) => {
      // The JSON parsing is now handled by the service
      if (event.eventType === 'PLAYER_JOINED') {
        console.log("Opponent joined event received! Navigating to lobby.");
        navigate(`/match/lobby/${data.matchId}`);
      }
    });

    return () => {
      console.log(`Cleaning up subscription for /topic/match/${data.matchId}`);
      
      // 3. --- THE FIX ---
      // Safely unsubscribe only if the subscription object was successfully created.
      if (subscription) {
        subscription.unsubscribe();
      }
    };
  }, [data.matchId, navigate]);

  const handleCopy = (text: string, type: 'link' | 'code') => {
    navigator.clipboard.writeText(text);
    setCopiedItem(type);
    setTimeout(() => setCopiedItem(null), 2000);
  };

  const inputGroupStyles = "flex items-center bg-zinc-800 border border-zinc-700 rounded-md";
  const textStyles = "flex-grow p-2 text-gray-300 overflow-x-auto whitespace-nowrap";
  const buttonStyles = "p-2 text-gray-400 hover:text-white transition-colors";

  return (
    <>
      <div className="text-center">
        <h2 className="text-3xl font-bold text-white mb-2">Match Ready!</h2>
        <p className="text-gray-400 mb-8">Share the code or link with your opponent.</p>
      </div>
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-400 mb-2">Room Code</label>
          <div className={inputGroupStyles}><p className={textStyles}>{data.roomCode}</p><button onClick={() => handleCopy(data.roomCode, 'code')} className={buttonStyles}>{copiedItem === 'code' ? <Check size={18} className="text-green-400" /> : <Copy size={18} />}</button></div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-400 mb-2">Shareable Link</label>
          <div className={inputGroupStyles}><p className={textStyles}>{data.shareableLink}</p><button onClick={() => handleCopy(data.shareableLink, 'link')} className={buttonStyles}>{copiedItem === 'link' ? <Check size={18} className="text-green-400" /> : <Copy size={18} />}</button></div>
        </div>
      </div>
      <div className="mt-8 text-center flex items-center justify-center gap-3 text-lg text-[#F97316]"><Loader2 className="animate-spin" /><span>Waiting for opponent to join...</span></div>
    </>
  );
};

export default CreateMatchPage;