import React, { useState, useEffect } from 'react';
import { useAuth } from '../../../core/hooks/useAuth';
import { Loader2 } from 'lucide-react';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake, FaAngleRight } from 'react-icons/fa';
import { getCurrentUserStats } from '../../match/services/matchService';
import { useChangePassword } from '../hooks/useChangePassword';
import { useToggle2FA } from '../hooks/useToggle2FA';
import type { UserStats } from '../../match/types/match';
import { useNavigate } from 'react-router-dom';
import { isStrongPassword } from '../../../core/utils/validators';

// --- Sub-Component for the Stats Section ---
const StatsSection = () => {
    const [stats, setStats] = useState<UserStats | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        getCurrentUserStats()
            .then(setStats)
            .catch(err => console.error("Failed to fetch stats:", err))
            .finally(() => setIsLoading(false));
    }, []);

    if (isLoading) {
        return <div className="flex justify-center p-8"><Loader2 className="animate-spin text-[#F97316]" /></div>;
    }
    if (!stats) return null;

    return (
        <div className="bg-zinc-900 border border-white/10 rounded-xl p-6">
            <div className="flex justify-between items-center mb-4">
                <h3 className="text-xl font-bold text-white">Your Duel Stats</h3>
                <button 
                    onClick={() => navigate('/matches/history')} 
                    className="text-sm font-semibold text-[#F97316] hover:text-[#EA580C] flex items-center gap-1 transition-colors"
                >
                    View Full History <FaAngleRight />
                </button>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                <div className="flex items-center gap-4"><FaGamepad className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Played</p><p className="text-2xl font-bold text-white">{stats.duelsPlayed}</p></div></div>
                <div className="flex items-center gap-4"><FaTrophy className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Won</p><p className="text-2xl font-bold text-white">{stats.duelsWon}</p></div></div>
                <div className="flex items-center gap-4"><FaTimesCircle className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Lost</p><p className="text-2xl font-bold text-white">{stats.duelsLost}</p></div></div>
                <div className="flex items-center gap-4"><FaHandshake className="text-[#F97316] text-3xl"/><div><p className="text-sm text-gray-400">Drawn</p><p className="text-2xl font-bold text-white">{stats.duelsDrawn}</p></div></div>
            </div>
        </div>
    );
};

// --- Sub-Component for the Security Section ---
const SecuritySection = () => {
    const { changePasswordMutation, isLoading: isPassLoading, error: apiError, success: apiSuccess, reset: resetApiState } = useChangePassword();
    const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
    const [validationError, setValidationError] = useState('');
    
    const { toggle2FAMutation, isLoading: is2faLoading, isEnabled: is2faEnabled } = useToggle2FA();

    const handlePasswordSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setValidationError('');
        resetApiState();

        if (passwords.newPassword !== passwords.confirmPassword) {
            setValidationError("New passwords do not match.");
            return;
        }
        if (!isStrongPassword(passwords.newPassword)) {
            setValidationError("Password must be 8+ chars and include uppercase, lowercase, number, & a special character.");
            return;
        }
        
        try {
            await changePasswordMutation({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
            setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });
        } catch (error) {
            console.error("Failed to change password:", error);
        }
    };
    
    const inputStyles = "w-full p-2 bg-zinc-800 border border-zinc-700 rounded-md focus:ring-2 focus:ring-[#F97316] focus:border-[#F97316] outline-none";
    const finalError = validationError || apiError;

    return (
        <div className="bg-zinc-900 border border-white/10 rounded-xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Security Settings</h3>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <form onSubmit={handlePasswordSubmit} className="space-y-4">
                    <h4 className="font-semibold text-white">Change Password</h4>
                    <div><input type="password" placeholder="Current Password" value={passwords.currentPassword} onChange={e => setPasswords(p => ({...p, currentPassword: e.target.value}))} className={inputStyles} required /></div>
                    <div><input type="password" placeholder="New Password" value={passwords.newPassword} onChange={e => setPasswords(p => ({...p, newPassword: e.target.value}))} className={inputStyles} required /></div>
                    <div><input type="password" placeholder="Confirm New Password" value={passwords.confirmPassword} onChange={e => setPasswords(p => ({...p, confirmPassword: e.target.value}))} className={inputStyles} required /></div>
                    
                    {finalError && <p className="text-xs text-red-500">{finalError}</p>}
                    {apiSuccess && !finalError && <p className="text-xs text-green-400">{apiSuccess}</p>}

                    <button type="submit" disabled={isPassLoading} className="w-full bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-md disabled:bg-orange-900/50">
                        {isPassLoading ? <Loader2 className="mx-auto animate-spin" /> : "Update Password"}
                    </button>
                </form>
                
                <div className="space-y-4">
                    <h4 className="font-semibold text-white">Two-Factor Authentication (2FA)</h4>
                    <p className="text-sm text-gray-400">Add an additional layer of security to your account during login.</p>
                    <div className="flex items-center justify-between bg-zinc-800 p-4 rounded-lg">
                        <p className="font-medium text-white">Status: <span className={is2faEnabled ? 'text-green-400' : 'text-red-500'}>{is2faEnabled ? 'Enabled' : 'Disabled'}</span></p>
                        <button onClick={() => toggle2FAMutation()} disabled={is2faLoading} className="bg-zinc-700 hover:bg-zinc-600 text-white font-semibold py-2 px-4 rounded-md disabled:opacity-50">
                            {is2faLoading ? <Loader2 className="animate-spin" /> : (is2faEnabled ? "Disable" : "Enable")}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

// --- Main Profile Page Component ---
const ProfilePage = () => {
    const { user } = useAuth();

    return (
        <div className="container mx-auto py-8 space-y-8 text-white">
            <div className="text-center">
                <h1 className="text-4xl font-bold capitalize">{user?.email.split('@')[0]}</h1>
                <p className="text-gray-400">{user?.email}</p>
            </div>
            <StatsSection />
            <SecuritySection />
        </div>
    );
};

export default ProfilePage;