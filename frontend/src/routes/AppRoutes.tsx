import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";

// Layout & Core Components
import MainLayout from "../components/layout/MainLayout";
import NotFoundPage from "../pages/NotFound";
import Home from "../pages/Home";
import AdminRoute from "../core/components/AdminRoute";

// Feature Page Imports
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
        <Route path="/matches" element={<MainLayout><div>Matches Page Content</div></MainLayout>} />
        <Route path="/profile" element={<MainLayout><div>Profile Page Content</div></MainLayout>} />
        <Route 
          path="/problems/:slug" 
          element={<MainLayout><ProblemPage /></MainLayout>} 
        />

        
        <Route path="/match/create" element={<CreateMatchPage />} />
        <Route path="/match/join" element={<JoinMatchPage />} />
        <Route path="/match/lobby/:matchId" element={<MatchLobbyPage />} />
        <Route path="/match/arena/:matchId" element={<MatchArenaPage />} />

        <Route path="/match/results/:matchId" element={<MatchResultsPage />} />


        <Route 
          path="/matches/history" 
          element={<MainLayout><MatchHistoryPage /></MainLayout>} 
        />
        
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Router>
  );
};

export default AppRoutes;