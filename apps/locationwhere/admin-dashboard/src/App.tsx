import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminLayout from './components/Layout/AdminLayout';
import LoginPage from './pages/Login/LoginPage';
import DashboardPage from './pages/Dashboard/DashboardPage';
import LiveMapPage from './pages/LiveMap/LiveMapPage';
import EmployeeListPage from './pages/Employees/EmployeeListPage';
import SimAlertsPage from './pages/SimAlerts/SimAlertsPage';
import RemoteControlPage from './pages/RemoteControl/RemoteControlPage';
import { useAuthStore } from './store/authStore';

const queryClient = new QueryClient();

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { accessToken } = useAuthStore();
  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="live-map" element={<LiveMapPage />} />
            <Route path="employees" element={<EmployeeListPage />} />
            <Route path="sim-alerts" element={<SimAlertsPage />} />
            <Route path="remote-control" element={<RemoteControlPage />} />
            {/* Other routes can be added similarly */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
