import { useForgotPassword } from '../hooks/useForgotPassword';
import RequestTokenForm from './RequestTokenForm';
import ResetPasswordForm from './ResetPasswordForm';

interface Props {
  theme: 'light' | 'dark';
}

const ForgotPasswordForm = ({ theme }: Props) => {
  const {
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
  } = useForgotPassword();

  if (step === 'reset') {
    return (
      <ResetPasswordForm
        token={token}
        setToken={setToken}
        newPassword={newPassword}
        setNewPassword={setNewPassword}
        confirmPassword={confirmPassword}
        setConfirmPassword={setConfirmPassword}
        onSubmit={handleResetSubmit}
        onResend={handleResendToken}
        loading={loading}
        error={error}
        success={success}
        theme={theme}
      />
    );
  }

  return (
    <RequestTokenForm
      email={email}
      setEmail={setEmail}
      onSubmit={handleRequestSubmit}
      loading={loading}
      error={error}
      success={success}
      theme={theme}
    />
  );
};

export default ForgotPasswordForm;

