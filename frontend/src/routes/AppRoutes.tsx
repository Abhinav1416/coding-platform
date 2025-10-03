import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import RegisterPage from "../features/auth/pages/RegisterPage";
import LoginPage from "../features/auth/pages/LoginPage";
import ForgotPasswordPage from "../features/auth/pages/ForgotPasswordPage";
import NotFoundPage from "../pages/NotFound";
import Home from "../pages/Home";
import MainLayout from "../components/layout/MainLayout";
import ProblemsPage from "../features/problem/pages/ProblemsPage";
import AdminRoute from "../core/components/AdminRoute";
import AdminDashboardPage from "../features/admin/pages/AdminDashboardPage";


const AppRoutes = () => {
  return (
    <Router>
      <Routes>

        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        <Route element={<AdminRoute />}>
          <Route 
            path="/admin" 
            element={<MainLayout><AdminDashboardPage /></MainLayout>} 
          />
        </Route>


        <Route path="/home" element={<MainLayout><Home /></MainLayout>} />
        <Route path="/problems" element={<MainLayout><ProblemsPage /></MainLayout>} />
        <Route path="/problems/:slug" element={<MainLayout><div>Individual Problem Detail Page</div></MainLayout>} />
        <Route path="/matches" element={<MainLayout><div>Matches Page Content</div></MainLayout>} />
        <Route path="/profile" element={<MainLayout><div>Profile Page Content</div></MainLayout>} />


        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Router>
  );
};

export default AppRoutes;