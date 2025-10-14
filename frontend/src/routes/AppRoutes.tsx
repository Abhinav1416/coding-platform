import { Routes, Route, Navigate } from "react-router-dom";
import MainLayout from "../components/layout/MainLayout";
import NotFoundPage from "../pages/NotFound";
import Home from "../pages/Home";
import AdminRoute from "../core/components/AdminRoute";
import RegisterPage from "../features/auth/pages/RegisterPage";
import LoginPage from "../features/auth/pages/LoginPage";
import ForgotPasswordPage from "../features/auth/pages/ForgotPasswordPage";
import AdminDashboardPage from "../features/admin/pages/AdminDashboardPage";
import ProblemPage from "../features/problem/pages/ProblemPage";
import CreateMatchPage from "../features/match/pages/CreateMatchPage";
import JoinMatchPage from "../features/match/pages/JoinMatchPage";
import MatchLobbyPage from "../features/match/pages/MatchLobbyPage";
import MatchArenaPage from "../features/match/pages/MatchArenaPage";
import MatchResultsPage from "../features/match/pages/MatchResultsPage";
import MatchHistoryPage from "../features/match/pages/MatchHistoryPage";
import ProfilePage from "../features/auth/pages/ProfilePage";

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />

      <Route
        element={<AdminRoute />}>
        <Route path="/admin"
          element={
            <MainLayout>
              <AdminDashboardPage />
            </MainLayout>
          }
        />
      </Route>
      
      <Route path="/home" element={<MainLayout><Home /></MainLayout>} />
      <Route path="/problems/:slug" element={<MainLayout><ProblemPage /></MainLayout>} />
      <Route path="/match/create" element={<CreateMatchPage />} />
      <Route path="/match/join" element={<JoinMatchPage />} />
      <Route path="/match/lobby/:matchId" element={<MatchLobbyPage />} />
      <Route path="/match/arena/:matchId" element={<MatchArenaPage />} />
      <Route path="/match/results/:matchId" element={<MatchResultsPage />} />
      <Route path="/matches/history" element={<MainLayout><MatchHistoryPage /></MainLayout>} />
      <Route path="/profile" element={<MainLayout><ProfilePage /></MainLayout>} />
      <Route path="*" element={<NotFoundPage />} />

    </Routes>
  );
};

export default AppRoutes;