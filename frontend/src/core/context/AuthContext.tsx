import React, { createContext, useState, useEffect, useCallback, useMemo, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';

import type { CredentialResponse } from '@react-oauth/google';
import { loginWithGoogle, fetchMyPermissions } from '../../features/auth/services/authService';

interface DecodedToken {
    sub: string;
    roles: string[];
    isTwoFactorEnabled: boolean;
    exp: number;
}

export interface AuthenticatedUser {
    email: string;
    roles: string[];
    twoFactorEnabled: boolean;
}

export interface AuthContextType {
    user: AuthenticatedUser | null;
    logout: () => void;
    isAuthenticated: boolean;
    permissions: string[];
    hasPermission: (permission: string) => boolean;
    hasRole: (role: string) => boolean;
    updateUser: (user: AuthenticatedUser) => void;
    googleLogin: (credentialResponse: CredentialResponse) => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<AuthenticatedUser | null>(null);
    const [permissions, setPermissions] = useState<string[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();

    const logout = useCallback(() => {
        setUser(null);
        setPermissions([]);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        navigate('/login');
    }, [navigate]);

    // Wrapped in useCallback to keep its memory reference stable
    const handleLoginSuccess = useCallback(async (accessToken: string, refreshToken?: string) => {
        localStorage.setItem('accessToken', accessToken);
        if (refreshToken) {
            localStorage.setItem('refreshToken', refreshToken);
        }

        const decoded = jwtDecode<DecodedToken>(accessToken);
        setUser({
            email: decoded.sub,
            roles: decoded.roles,
            twoFactorEnabled: decoded.isTwoFactorEnabled,
        });

        try {
            const fetchedPermissions = await fetchMyPermissions();
            setPermissions(fetchedPermissions);
        } catch (error) {
            console.error("Failed to fetch permissions", error);
        }
        
        navigate('/');
    }, [navigate]);

    // Wrapped in useCallback
    const googleLogin = useCallback(async (credentialResponse: CredentialResponse) => {
        try {
            if (!credentialResponse.credential) {
                throw new Error("Google login failed: No credential returned.");
            }
            const { accessToken, refreshToken } = await loginWithGoogle(credentialResponse.credential);
            
            if (!accessToken) {
                throw new Error("Login failed: No access token received from backend.");
            }

            await handleLoginSuccess(accessToken, refreshToken ?? undefined);
        } catch (error) {
            console.error("Error during Google login:", error);
            logout();
        }
    }, [handleLoginSuccess, logout]);

    useEffect(() => {
        const initializeAuth = async () => {
            const token = localStorage.getItem('accessToken');
            
            if (!token) {
                setIsLoading(false);
                return;
            }

            try {
                const decoded = jwtDecode<DecodedToken>(token);
                // Check if token is expired
                if (decoded.exp * 1000 > Date.now()) {
                    setUser({ 
                        email: decoded.sub, 
                        roles: decoded.roles,
                        twoFactorEnabled: decoded.isTwoFactorEnabled
                    });
                    const fetchedPermissions = await fetchMyPermissions();
                    setPermissions(fetchedPermissions);
                } else {
                    logout();
                }
            } catch (error) {
                console.error("Invalid token during initialization:", error);
                logout();
            }
            
            setIsLoading(false);
        };
        initializeAuth();
    }, [logout]);

    // Wrapped in useCallback
    const hasPermission = useCallback((permission: string): boolean => {
        return permissions.includes(permission);
    }, [permissions]);

    // Wrapped in useCallback
    const hasRole = useCallback((role: string): boolean => {
        return user?.roles.includes(role) ?? false;
    }, [user?.roles]);
    
    // The Ultimate Fix: Memoize the context value
    const contextValue = useMemo<AuthContextType>(() => ({
        user,
        logout,
        isAuthenticated: !!user,
        permissions,
        hasPermission,
        hasRole,
        updateUser: setUser,
        googleLogin,
    }), [user, permissions, logout, hasPermission, hasRole, googleLogin]);
    
    if (isLoading) {
        return <div className="min-h-screen bg-gray-900 flex items-center justify-center text-white">Loading Application...</div>;
    }

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
};