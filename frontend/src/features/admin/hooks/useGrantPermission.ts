import { useMutation } from '@tanstack/react-query';
import { toast } from 'react-hot-toast';
import * as adminService from '../services/adminService';

export const useGrantPermission = () => {


  const { mutate: grantCreate, isPending: isCreating } = useMutation({
    mutationFn: adminService.grantCreatePermission,
    onSuccess: (data) => {
      toast.success(data.message || 'Create permission granted successfully!');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to grant create permission.');
    },
  });


  const { mutate: grantUpdate, isPending: isUpdating } = useMutation({
    mutationFn: adminService.grantUpdatePermission,
    onSuccess: (data) => {
      toast.success(data.message || 'Update permission granted successfully!');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to grant update permission.');
    },
  });


  const { mutate: grantDelete, isPending: isDeleting } = useMutation({
    mutationFn: adminService.grantDeletePermission,
    onSuccess: (data) => {
      toast.success(data.message || 'Delete permission granted successfully!');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to grant delete permission.');
    },
  });

  return {
    grantCreate,
    isCreating,
    grantUpdate,
    isUpdating,
    grantDelete,
    isDeleting,
  };
};