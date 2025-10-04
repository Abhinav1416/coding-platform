import React from 'react';
import { useForm, type SubmitHandler } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useGrantPermission } from '../hooks/useGrantPermission';
import { 
  grantCreateSchema, 
  grantScopedSchema, 
  type GrantCreateFormValues, 
  type GrantScopedFormValues 
} from '../types/admin';



const FormInput = ({ id, label, register, error, ...rest }: any) => (
  <div>
    <label htmlFor={id} className="block text-sm font-medium text-gray-300 mb-1">{label}</label>
    <input
      id={id}
      className="w-full bg-zinc-800 border border-zinc-600 rounded-md px-3 py-2 text-white focus:ring-2 focus:ring-[#F97316] focus:outline-none"
      {...register(id)}
      {...rest}
    />
    {error && <p className="text-red-400 text-xs mt-1">{error.message}</p>}
  </div>
);



const GrantScopedPermissionForm: React.FC<{
  title: string;
  onSubmit: SubmitHandler<GrantScopedFormValues>;
  isLoading: boolean;
}> = ({ title, onSubmit, isLoading }) => {
  const { register, handleSubmit, formState: { errors }, reset } = useForm<GrantScopedFormValues>({
    resolver: zodResolver(grantScopedSchema),
  });

  const handleFormSubmit = (data: GrantScopedFormValues) => {
    onSubmit(data);
    reset();
  };
  
  return (
    <div className="bg-zinc-900 p-6 rounded-lg border border-white/10">
      <h3 className="text-xl font-semibold mb-4 text-white">{title}</h3>
      <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
        <FormInput id="email" label="User Email" register={register} error={errors.email} />
        <FormInput id="problemId" label="Problem ID (UUID)" register={register} error={errors.problemId} />
        <button type="submit" disabled={isLoading} className="w-full bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-md transition-colors disabled:opacity-50">
          {isLoading ? 'Granting...' : 'Grant Permission'}
        </button>
      </form>
    </div>
  );
};


const AdminDashboardPage: React.FC = () => {
  const { grantCreate, isCreating, grantUpdate, isUpdating, grantDelete, isDeleting } = useGrantPermission();
  
  const { register: registerCreate, handleSubmit: handleSubmitCreate, formState: { errors: errorsCreate }, reset: resetCreate } = useForm<GrantCreateFormValues>({
    resolver: zodResolver(grantCreateSchema),
  });

  const handleCreateSubmit: SubmitHandler<GrantCreateFormValues> = (data) => {
    grantCreate(data);
    resetCreate();
  };

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-4xl font-bold text-white">Admin Dashboard</h1>
        <p className="text-gray-400 mt-2">Manage user permissions for problem contribution.</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="bg-zinc-900 p-6 rounded-lg border border-white/10">
          <h3 className="text-xl font-semibold mb-4 text-white">Grant Create Permission</h3>
          <form onSubmit={handleSubmitCreate(handleCreateSubmit)} className="space-y-4">
            <FormInput id="email" label="User Email" register={registerCreate} error={errorsCreate.email} />
            <button type="submit" disabled={isCreating} className="w-full bg-[#F97316] hover:bg-[#EA580C] text-white font-bold py-2 px-4 rounded-md transition-colors disabled:opacity-50">
              {isCreating ? 'Granting...' : 'Grant Permission'}
            </button>
          </form>
        </div>

        <GrantScopedPermissionForm
          title="Grant Update Permission"
          onSubmit={(data) => grantUpdate(data)}
          isLoading={isUpdating}
        />

        <GrantScopedPermissionForm
          title="Grant Delete Permission"
          onSubmit={(data) => grantDelete(data)}
          isLoading={isDeleting}
        />
      </div>
    </div>
  );
};

export default AdminDashboardPage;