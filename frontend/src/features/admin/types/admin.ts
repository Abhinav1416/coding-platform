import { z } from 'zod';


export const grantCreateSchema = z.object({
  email: z.string().email({ message: 'Invalid email address' }),
});


export const grantScopedSchema = z.object({
  email: z.string().email({ message: 'Invalid email address' }),
  problemId: z.string().uuid({ message: 'Must be a valid UUID' }),
});


export type GrantCreateFormValues = z.infer<typeof grantCreateSchema>;
export type GrantScopedFormValues = z.infer<typeof grantScopedSchema>;