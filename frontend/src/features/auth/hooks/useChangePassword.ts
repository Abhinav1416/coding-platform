import { useState, useCallback } from 'react';
import { changePassword } from '../services/authService';
import type { ChangePasswordRequest } from '../types/auth';

export const useChangePassword = () => {
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const changePasswordMutation = async (data: ChangePasswordRequest) => {
        setIsLoading(true);
        setError(null);
        setSuccess(null);
        try {
            await changePassword(data);
            setSuccess("Password updated successfully!");
        } catch (err: any) {
            setError(err.response?.data?.message || "An error occurred.");
            throw err;
        } finally {
            setIsLoading(false);
        }
    };
    
    const reset = useCallback(() => {
        setError(null);
        setSuccess(null);
    }, []);

    return { 
        changePasswordMutation, 
        isLoading, 
        error, 
        success, 
        reset
    };
};