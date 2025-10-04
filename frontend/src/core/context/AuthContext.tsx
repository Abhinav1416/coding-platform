import React, { createContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { fetchMyPermissions } from '../../features/auth/services/authService';


interface DecodedToken {
    email: string;
    roles: string[];
    exp: number;
}


interface User {
    email: string;
    roles: string[];
}


interface AuthContextType {
    user: User | null;
    logout: () => void;
    isAuthenticated: boolean;
    permissions: string[];
    hasPermission: (permission: string) => boolean;
    hasRole: (role: string) => boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
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
                        setUser({ email: decoded.email, roles: decoded.roles });
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
    };
    
    if (isLoading) {
        return <div>Loading...</div>;
    }

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
};