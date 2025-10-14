import React, { useState, useEffect } from 'react';
import { useAuth } from '../../../core/hooks/useAuth';
import { Loader2 } from 'lucide-react';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake, FaAngleRight, FaShieldAlt } from 'react-icons/fa';
import { getCurrentUserStats } from '../../match/services/matchService';
import { useChangePassword } from '../hooks/useChangePassword';
import { useToggle2FA } from '../hooks/useToggle2FA';
import type { UserStats } from '../../match/types/match';
import { useNavigate } from 'react-router-dom';
import { isStrongPassword } from '../../../core/utils/validators';


const SectionCard: React.FC<{ title: string; icon: React.ReactNode; children: React.ReactNode; action?: React.ReactNode }> = ({ title, icon, children, action }) => (
    <div className="bg-zinc-900 border border-white/10 rounded-xl shadow-lg">
        <div className="flex justify-between items-center p-4 sm:p-6 border-b border-white/10">
            <div className="flex items-center gap-3">
                <span className="text-[#F97316]">{icon}</span>
                <h3 className="text-lg font-bold text-white">{title}</h3>
            </div>
            {action}
        </div>
        <div className="p-4 sm:p-6">
            {children}
        </div>
    </div>
);


const StatCard: React.FC<{ icon: React.ReactNode; label: string; value: number | string }> = ({ icon, label, value }) => (
    <div className="bg-zinc-800/50 p-4 rounded-lg flex items-center gap-4 transition-transform hover:-translate-y-1">
        <div className="text-[#F97316] text-3xl">{icon}</div>
        <div>
            <p className="text-sm text-gray-400">{label}</p>
            <p className="text-2xl font-bold text-white">{value}</p>
        </div>
    </div>
);


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

    const statsData = stats ? [
        { icon: <FaGamepad />, label: "Played", value: stats.duelsPlayed },
        { icon: <FaTrophy />, label: "Won", value: stats.duelsWon },
        { icon: <FaTimesCircle />, label: "Lost", value: stats.duelsLost },
        { icon: <FaHandshake />, label: "Drawn", value: stats.duelsDrawn },
    ] : [];

    const actionButton = (
        <button 
            onClick={() => navigate('/matches/history')} 
            className="text-sm font-semibold text-[#F97316] hover:text-[#EA580C] flex items-center gap-1 transition-colors"
        >
            View History <FaAngleRight />
        </button>
    );

    return (
        <SectionCard title="Duel Stats" icon={<FaTrophy size={20} />} action={actionButton}>
            {isLoading && <div className="flex justify-center p-8"><Loader2 className="animate-spin text-[#F97316]" size={24} /></div>}
            {!isLoading && stats && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                    {statsData.map(stat => <StatCard key={stat.label} {...stat} />)}
                </div>
            )}
        </SectionCard>
    );
};


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

            await changePasswordMutation({ 
                currentPassword: passwords.currentPassword, 
                newPassword: passwords.newPassword 
            });
            

            setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });

        } catch (error) {

            console.error("Failed to change password:", error);
        }
    };
    
    const inputStyles = "w-full p-2 bg-zinc-800 border border-zinc-700 rounded-md focus:ring-1 focus:ring-[#F97316] focus:border-[#F97316] outline-none transition-colors";
    const labelStyles = "block text-sm font-medium text-gray-300 mb-1";
    const finalError = validationError || apiError;

    return (
        <SectionCard title="Security Settings" icon={<FaShieldAlt size={20} />}>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-x-8 gap-y-10">

                <form onSubmit={handlePasswordSubmit} className="space-y-4 flex flex-col">
                    <h4 className="font-semibold text-lg text-white">Change Password</h4>
                    <div>
                        <label htmlFor="currentPassword" className={labelStyles}>Current Password</label>
                        <input id="currentPassword" type="password" value={passwords.currentPassword} onChange={e => setPasswords(p => ({...p, currentPassword: e.target.value}))} className={inputStyles} required />
                    </div>
                    <div>
                        <label htmlFor="newPassword" className={labelStyles}>New Password</label>
                        <input id="newPassword" type="password" value={passwords.newPassword} onChange={e => setPasswords(p => ({...p, newPassword: e.target.value}))} className={inputStyles} required />
                    </div>
                    <div>
                        <label htmlFor="confirmPassword" className={labelStyles}>Confirm New Password</label>
                        <input id="confirmPassword" type="password" value={passwords.confirmPassword} onChange={e => setPasswords(p => ({...p, confirmPassword: e.target.value}))} className={inputStyles} required />
                    </div>
                    
                    <div className="h-4 mt-1">
                        {finalError && <p className="text-xs text-red-500">{finalError}</p>}
                        {apiSuccess && !finalError && <p className="text-xs text-green-400">{apiSuccess}</p>}
                    </div>

                    <button type="submit" disabled={isPassLoading} className="mt-auto w-full bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2.5 px-4 rounded-md disabled:bg-orange-900/50 transition-colors">
                        {isPassLoading ? <Loader2 className="mx-auto animate-spin" /> : "Update Password"}
                    </button>
                </form>
                

                <div className="space-y-4">
                     <h4 className="font-semibold text-lg text-white">Two-Factor Authentication (2FA)</h4>
                    <p className="text-sm text-gray-400">Add an additional layer of security to your account during login.</p>
                    <div className="flex items-center justify-between bg-zinc-800/50 p-4 rounded-lg border border-zinc-700">
                        <div className="flex items-center gap-2">
                           <p className="font-medium text-white">Status:</p>
                           <span className={`px-2.5 py-0.5 text-xs font-semibold rounded-full ${is2faEnabled ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                                {is2faEnabled ? 'Enabled' : 'Disabled'}
                           </span>
                        </div>
                        <button onClick={() => toggle2FAMutation()} disabled={is2faLoading} className="bg-zinc-700 hover:bg-zinc-600 text-white font-semibold py-2 px-4 rounded-md disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
                            {is2faLoading ? <Loader2 className="animate-spin" /> : (is2faEnabled ? "Disable" : "Enable")}
                        </button>
                    </div>
                </div>
            </div>
        </SectionCard>
    );
};

const ProfilePage = () => {
    const { user } = useAuth();
    const username = user?.email.split('@')[0] || 'User';

    return (
        <div className="container mx-auto max-w-5xl px-4 py-8 sm:py-12 space-y-8 text-white">

            <div className="flex items-center gap-4 sm:gap-6">
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-[#F97316] flex items-center justify-center text-zinc-900 text-3xl sm:text-4xl font-bold">
                    {username.charAt(0).toUpperCase()}
                </div>
                <div>
                    <h1 className="text-3xl sm:text-4xl font-bold capitalize">{username}</h1>
                    <p className="text-gray-400 text-sm sm:text-base">{user?.email}</p>
                </div>
            </div>
            
            
            <StatsSection />
            <SecuritySection />
        </div>
    );
};

export default ProfilePage;
