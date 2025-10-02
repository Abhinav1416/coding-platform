import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import RegisterPage from "../features/auth/pages/RegisterPage";
import LoginPage from "../features/auth/pages/LoginPage";
import ForgotPasswordPage from "../features/auth/pages/ForgotPasswordPage";

import NotFoundPage from "../pages/NotFound";
import HomePage from "../pages/Home";
import MainLayout from "../components/layout/MainLayout";

const AppRoutes = () => {
  return (
    <Router>
      <Routes>
        {/* Public Routes (no layout) */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        {/* Protected/Layout Routes */}
        <Route path="/home" element={<MainLayout><HomePage /></MainLayout>} />
        <Route path="/problems" element={<MainLayout><div>Problems Page Content</div></MainLayout>} />
        <Route path="/matches" element={<MainLayout><div>Matches Page Content</div></MainLayout>} />
        <Route path="/profile" element={<MainLayout><div>Profile Page Content</div></MainLayout>} />

        {/* 404 Page */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Router>
  );
};

export default AppRoutes;
