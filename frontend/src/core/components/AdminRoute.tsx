import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';

interface DecodedToken {
  sub: string;
  roles?: string[] | string;
  authorities?: string[] | string;
  iat: number;
  exp: number;
}

const useAdminAuth = (): boolean => {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    console.error("DEBUG: No access token found in local storage. Redirecting.");
    return false;
  }

  try {
    const decodedToken = jwtDecode<DecodedToken>(token);


    const userPermissions = decodedToken.roles || decodedToken.authorities;
    
    if (!userPermissions) {
        console.error("DEBUG: Token found, but 'roles' or 'authorities' claim is missing.", decodedToken);
        return false;
    }

    const permissionsArray = Array.isArray(userPermissions) ? userPermissions : [userPermissions];
    

    const isAdmin = permissionsArray.includes('ROLE_ADMIN');
    
    if (!isAdmin) {
        console.log("DEBUG: User is not an admin. Permissions found:", permissionsArray);
    } else {
        console.log("DEBUG: Admin check successful. Permissions found:", permissionsArray);
    }

    return isAdmin;

  } catch (error) {
    console.error("DEBUG: Failed to decode token. It might be invalid or expired. Redirecting.", error);
    return false;
  }
};

const AdminRoute: React.FC = () => {
  const isAdmin = useAdminAuth();
  return isAdmin ? <Outlet /> : <Navigate to="/not-found" replace />;
};

export default AdminRoute;