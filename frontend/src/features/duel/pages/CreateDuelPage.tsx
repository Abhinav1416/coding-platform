import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, Plus, Trash2, Swords, Info, AlertCircle, Trophy } from 'lucide-react';
import { toast } from 'react-hot-toast';
import MainLayout from '../../../components/layout/MainLayout';
import { duelService } from '../api/duelService';
import { useAuth } from '../../../core/hooks/useAuth';

const CF_REGEX = /codeforces\.com\/(contest|gym|problemset\/problem)\/(\d+)(?:\/problem)?\/([A-Z][0-9]?)/;

const CreateDuelPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(false);
  
  const [formData, setFormData] = useState({
    handle: '', 
    problemLinks: [''],
    startsInMinutes: 2,
    durationMinutes: 5,
  });

  const handleLinkChange = (index: number, value: string) => {
    const newLinks = [...formData.problemLinks];
    newLinks[index] = value;
    setFormData({ ...formData, problemLinks: newLinks });
  };

  const addLinkField = () => {
    if (formData.problemLinks.length < 4) {
      setFormData({ ...formData, problemLinks: [...formData.problemLinks, ''] });
    }
  };

  const removeLinkField = (index: number) => {
    const newLinks = formData.problemLinks.filter((_, i) => i !== index);
    setFormData({ ...formData, problemLinks: newLinks });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      toast.error("You must be logged in to create a duel.");
      navigate('/login');
      return;
    }

    if (!formData.handle.trim()) return toast.error("Please enter your CF Handle");
    
    const validLinks = formData.problemLinks.filter(l => l.trim().length > 0);
    if (validLinks.length === 0) return toast.error("Add at least one problem link");

    const invalidLinks = validLinks.filter(link => !CF_REGEX.test(link));
    if (invalidLinks.length > 0) {
      return toast.error("Invalid URL format. Use standard Codeforces problem links.");
    }

    setIsLoading(true);
    try {
      const response = await duelService.createDuel({
        ...formData,
        problemLinks: validLinks
      });
      toast.success("Duel Created!");
      navigate(`/duel/lobby/${response.duelId}`);
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Failed to create duel';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <MainLayout>
      <div className="flex items-center justify-center pt-10 pb-20 px-4">
        <div className="w-full max-w-2xl">
          
          <div className="bg-white dark:bg-zinc-900 p-8 rounded-xl border border-gray-200 dark:border-white/10 shadow-lg mb-8">
            <div className="flex flex-col items-center mb-8">
              <Swords className="text-[#F97316] w-12 h-12 mb-4" />
              <h2 className="text-3xl font-bold text-center text-gray-900 dark:text-white">Create Code Duel</h2>
              <p className="text-gray-500 dark:text-gray-400 mt-2 text-center">Challenge a friend to a 1v1 coding battle.</p>
            </div>
            
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Your Codeforces Handle</label>
                <input 
                  required
                  className="w-full p-3 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-white rounded-md outline-none focus:border-[#F97316] transition-colors"
                  value={formData.handle}
                  onChange={e => setFormData({...formData, handle: e.target.value})}
                  placeholder="e.g. tourist"
                />
                <div className="flex gap-2 mt-2 text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 p-3 rounded-md text-xs">
                  <Info size={16} className="shrink-0" />
                  <p>
                    <strong>Why is this important?</strong> We track your submissions in real-time. 
                    If you enter the wrong handle, your solutions <u>will not count</u> even if you solve them on Codeforces.
                  </p>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Problem Links (1-4)</label>
                <div className="space-y-2">
                  {formData.problemLinks.map((link, idx) => (
                    <div key={idx} className="flex gap-2">
                      <input
                        className="flex-1 p-3 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-white rounded-md outline-none focus:border-[#F97316] text-sm"
                        value={link}
                        onChange={e => handleLinkChange(idx, e.target.value)}
                        placeholder="https://codeforces.com/problemset/problem/..."
                      />
                      {formData.problemLinks.length > 1 && (
                        <button type="button" onClick={() => removeLinkField(idx)} className="p-3 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md transition-colors">
                          <Trash2 size={20} />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                {formData.problemLinks.length < 4 && (
                  <button type="button" onClick={addLinkField} className="flex items-center gap-2 text-[#F97316] text-sm font-semibold hover:text-orange-600 mt-3 transition-colors">
                    <Plus size={16} /> Add another problem
                  </button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Starts In (min)</label>
                  <input 
                    type="number" min="1" max="5"
                    className="w-full p-3 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-white rounded-md outline-none focus:border-[#F97316]"
                    value={formData.startsInMinutes}
                    onChange={e => setFormData({...formData, startsInMinutes: parseInt(e.target.value)})}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-400 mb-2">Duration (min)</label>
                  <input 
                    type="number" min="5" max="90"
                    className="w-full p-3 bg-gray-50 dark:bg-zinc-800 border border-gray-300 dark:border-zinc-700 text-gray-900 dark:text-white rounded-md outline-none focus:border-[#F97316]"
                    value={formData.durationMinutes}
                    onChange={e => setFormData({...formData, durationMinutes: parseInt(e.target.value)})}
                  />
                </div>
              </div>

              <button 
                type="submit" 
                disabled={isLoading}
                className="w-full flex justify-center items-center bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-3 px-4 rounded-md transition-transform transform hover:scale-105 disabled:bg-orange-900/50 disabled:cursor-not-allowed disabled:transform-none"
              >
                {isLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Create Duel Room'}
              </button>
            </form>
          </div>

          <div className="bg-orange-50 dark:bg-zinc-800/50 border border-orange-100 dark:border-zinc-700 rounded-xl p-6">
            <h3 className="text-lg font-bold text-gray-900 dark:text-white flex items-center gap-2 mb-4">
              <Trophy size={20} className="text-[#F97316]" /> Rules of Engagement
            </h3>
            <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
              <li className="flex gap-2"><AlertCircle size={16} className="shrink-0 mt-0.5" /> <strong>Scoring:</strong> ICPC Format. Most solved problems wins.</li>
              <li className="flex gap-2"><AlertCircle size={16} className="shrink-0 mt-0.5" /> <strong>Penalty:</strong> If scores are tied, lowest time penalty wins. (Time taken + 20min per wrong submission).</li>
              <li className="flex gap-2"><AlertCircle size={16} className="shrink-0 mt-0.5" /> <strong>Submission:</strong> You must submit directly on Codeforces during the match.</li>
            </ul>
          </div>

        </div>
      </div>
    </MainLayout>
  );
};

export default CreateDuelPage;