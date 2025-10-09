import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../../core/hooks/useAuth';
import { toggle2FA } from '../services/authService';
import type { AuthResponse } from '../types/auth';

export const useToggle2FA = () => {
    const { user } = useAuth();
    const queryClient = useQueryClient();

    const mutation = useMutation<AuthResponse, Error>({
        mutationFn: toggle2FA,
        onSuccess: (data) => {
            if (data.accessToken) {
                localStorage.setItem('accessToken', data.accessToken);
            }
            queryClient.invalidateQueries({ queryKey: ['permissions'] });
            window.location.reload();
        },
        onError: (error) => {
            console.error("Failed to toggle 2FA:", error);
        },
    });

    return {
        toggle2FAMutation: mutation.mutate,
        isLoading: mutation.isPending,
        isEnabled: user?.twoFactorEnabled,
    };
};