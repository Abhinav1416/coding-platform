import { useMemo, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchMyPermissions } from '../../features/auth/services/authService';

interface DecodedToken {
  sub: string;
  roles: string[];
  isTwoFactorEnabled: boolean;
  iat: number;
  exp: number;
}

export const useAuth = () => {
  const token = localStorage.getItem('accessToken');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const decodedToken = useMemo<DecodedToken | null>(() => {
    if (!token) return null;
    try {
      const decoded = jwtDecode<DecodedToken>(token);

      if (decoded.exp * 1000 < Date.now()) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        return null;
      }
      return decoded;
    } catch (error) {
      console.error("Failed to decode token", error);
      return null;
    }
  }, [token]);

  const { data: permissionData } = useQuery({
    queryKey: ['permissions', decodedToken?.sub],
    queryFn: fetchMyPermissions, 
    enabled: !!decodedToken,
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: true, 
  });


  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    queryClient.invalidateQueries({ queryKey: ['permissions'] });
    navigate('/login');
  }, [navigate, queryClient]);


  const hasPermission = (permission: string, scopeId?: string): boolean => {
    if (!permissionData || permissionData.length === 0) {
      return false;
    }
    const permissionToCheck = scopeId ? `${permission}:${scopeId}` : permission;
    return permissionData.includes(permissionToCheck);
  };

  const hasRole = (role: string): boolean => {
    if (!decodedToken?.roles) {
      return false;
    }
    return decodedToken.roles.includes(role);
  };

  return { 
    isAuthenticated: !!decodedToken,
    user: decodedToken 
      ? { 
          email: decodedToken.sub, 
          roles: decodedToken.roles,
          twoFactorEnabled: decodedToken.isTwoFactorEnabled 
        } 
      : null,
    permissions: permissionData || [],
    hasPermission,
    hasRole,
    logout,
  };
};