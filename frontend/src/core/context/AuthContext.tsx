import React, { createContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { fetchMyPermissions } from '../../features/auth/services/authService';

// This is the expected structure of the decoded JWT payload
interface DecodedToken {
    sub: string; // 'sub' is the standard claim for user's email/username
    roles: string[];
    isTwoFactorEnabled: boolean; // ✅ Your backend should include this in the JWT
    exp: number;
}

// This represents the authenticated user object available in the app
export interface AuthenticatedUser {
    email: string;
    roles: string[];
    twoFactorEnabled: boolean; // ✅ Added to track 2FA status
}

interface AuthContextType {
    user: AuthenticatedUser | null;
    logout: () => void;
    isAuthenticated: boolean;
    permissions: string[];
    hasPermission: (permission: string) => boolean;
    hasRole: (role: string) => boolean;
    updateUser: (user: AuthenticatedUser) => void; // ✅ Added function to update user state
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

    useEffect(() => {
        const initializeAuth = async () => {
            const token = localStorage.getItem('accessToken');
            if (token) {
                try {
                    const decoded = jwtDecode<DecodedToken>(token);
                    if (decoded.exp * 1000 > Date.now()) {
                        setUser({ 
                            email: decoded.sub, 
                            roles: decoded.roles,
                            twoFactorEnabled: decoded.isTwoFactorEnabled // ✅ Set 2FA status from token
                        });
                        const fetchedPermissions = await fetchMyPermissions();
                        setPermissions(fetchedPermissions);
                    } else {
                        logout();
                    }
                } catch (error) {
                    console.error("Invalid token:", error);
                    logout();
                }
            }
            setIsLoading(false);
        };
        initializeAuth();
    }, [logout]);

    const hasPermission = (permission: string): boolean => {
        return permissions.includes(permission);
    };

    const hasRole = (role: string): boolean => {
        return user?.roles.includes(role) ?? false;
    };
    
    const contextValue: AuthContextType = {
        user,
        logout,
        isAuthenticated: !!user,
        permissions,
        hasPermission,
        hasRole,
        updateUser: setUser, // ✅ Expose the setUser function
    };
    
    if (isLoading) {
        // You might want a better loading spinner here
        return <div className="min-h-screen bg-gray-900 flex items-center justify-center text-white">Loading Application...</div>;
    }

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
};