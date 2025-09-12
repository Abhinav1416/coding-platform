import { useState } from 'react';
import { isValidEmail } from '../../../core/utils/validators';
import { sendPasswordResetToken, resetPassword as resetPasswordService } from '../services/authService';

export const useForgotPassword = () => {
  const [step, setStep] = useState<'request' | 'reset'>('request');
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const sendToken = async () => {
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      await sendPasswordResetToken({ email });
      return true;
    } catch (err: any) {
      setError(err.message || 'An unexpected error occurred.');
      return false;
    } finally {
      setLoading(false);
    }
  };

  const handleRequestSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValidEmail(email)) {
      setError('Please enter a valid email address.');
      return;
    }
    const wasSuccessful = await sendToken();
    if (wasSuccessful) {
      setSuccess('A password reset token has been sent to your email.');
      setStep('reset');
    }
  };
  
  const handleResendToken = async () => {
    const wasSuccessful = await sendToken();
    if (wasSuccessful) {
      setSuccess('A new token has been sent to your email.');
    }
    setToken('');
    setNewPassword('');
    setConfirmPassword('');
  };

  const handleResetSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (!newPassword || newPassword.length < 8) {
      setError(
        'Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.'
      );
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    
    try {
      await resetPasswordService({ email, token, newPassword });
      setSuccess('Your password has been reset successfully! You can now log in.');
      setStep('request');
      setToken('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      setError(err.message || 'Failed to reset password. The token may be invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  return {
    step,
    email,
    setEmail,
    token,
    setToken,
    newPassword,
    setNewPassword,
    confirmPassword,
    setConfirmPassword,
    error,
    success,
    loading,
    handleRequestSubmit,
    handleResetSubmit,
    handleResendToken,
  };
};

