import React, { useState, useEffect } from 'react';
import { useAuth } from '../../../core/hooks/useAuth';
import { useTheme } from '../../../core/context/ThemeContext';
import { Loader2 } from 'lucide-react';
import { FaGamepad, FaTrophy, FaTimesCircle, FaHandshake, FaAngleRight } from 'react-icons/fa';
import { getCurrentUserStats } from '../../match/services/matchService';
import type { UserStats } from '../../match/types/match';
import { useNavigate } from 'react-router-dom';

const SectionCard: React.FC<{ title: string; icon: React.ReactNode; children: React.ReactNode; action?: React.ReactNode; isDark: boolean }> = ({ title, icon, children, action, isDark }) => (
    <div className={`${isDark ? 'bg-zinc-900 border-white/10' : 'bg-white border-gray-200 shadow-sm'} border rounded-xl transition-colors duration-300`}>
        <div className={`flex justify-between items-center p-4 sm:p-6 border-b ${isDark ? 'border-white/10' : 'border-gray-100'}`}>
            <div className="flex items-center gap-3">
                <span className="text-[#F97316]">{icon}</span>
                <h3 className={`text-lg font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>{title}</h3>
            </div>
            {action}
        </div>
        <div className="p-4 sm:p-6">
            {children}
        </div>
    </div>
);

const StatCard: React.FC<{ icon: React.ReactNode; label: string; value: number | string; isDark: boolean }> = ({ icon, label, value, isDark }) => (
    <div className={`${isDark ? 'bg-zinc-800/50' : 'bg-gray-50 border border-gray-100'} p-4 rounded-lg flex items-center gap-4 transition-transform hover:-translate-y-1`}>
        <div className="text-[#F97316] text-3xl">{icon}</div>
        <div>
            <p className={`text-sm ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>{label}</p>
            <p className={`text-2xl font-bold ${isDark ? 'text-white' : 'text-gray-900'}`}>{value}</p>
        </div>
    </div>
);

const StatsSection = ({ isDark }: { isDark: boolean }) => {
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
        <SectionCard title="Duel Stats" icon={<FaTrophy size={20} />} action={actionButton} isDark={isDark}>
            {isLoading && <div className="flex justify-center p-8"><Loader2 className="animate-spin text-[#F97316]" size={24} /></div>}
            {!isLoading && stats && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                    {statsData.map(stat => <StatCard key={stat.label} {...stat} isDark={isDark} />)}
                </div>
            )}
        </SectionCard>
    );
};

const ProfilePage = () => {
    const { user } = useAuth();
    const { theme } = useTheme();
    const isDark = theme === 'dark';
    const username = user?.email.split('@')[0] || 'User';

    return (
        <div className={`container mx-auto max-w-5xl px-4 py-8 sm:py-12 space-y-8 transition-colors duration-300 ${isDark ? 'text-white' : 'text-gray-900'}`}>
            <div className="flex items-center gap-4 sm:gap-6">
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-[#F97316] flex items-center justify-center text-white text-3xl sm:text-4xl font-bold shadow-lg shadow-orange-500/20">
                    {username.charAt(0).toUpperCase()}
                </div>
                <div>
                    <h1 className={`text-3xl sm:text-4xl font-bold capitalize ${isDark ? 'text-white' : 'text-gray-900'}`}>{username}</h1>
                    <p className={`text-sm sm:text-base ${isDark ? 'text-gray-400' : 'text-gray-500'}`}>{user?.email}</p>
                </div>
            </div>
            <StatsSection isDark={isDark} />
        </div>
    );
};

export default ProfilePage;