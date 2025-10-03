import api from '../../../core/api/api';


export const grantCreatePermission = async (data: { email: string }) => {
  const response = await api.post('/api/v1/admin/permissions/grant-create', data);
  return response.data;
};


export const grantUpdatePermission = async (data: { email: string; problemId: string }) => {
  const response = await api.post('/api/v1/admin/permissions/grant-update', data);
  return response.data;
};


export const grantDeletePermission = async (data: { email: string; problemId: string }) => {
  const response = await api.post('/api/v1/admin/permissions/grant-delete', data);
  return response.data;
};