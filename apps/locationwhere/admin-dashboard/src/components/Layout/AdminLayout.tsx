import React, { useState } from 'react';
import { Layout, Menu, Button, theme, Dropdown, Space, Avatar } from 'antd';
import {
  DashboardOutlined,
  EnvironmentOutlined,
  UserOutlined,
  HistoryOutlined,
  PhoneOutlined,
  AlertOutlined,
  SafetyCertificateOutlined,
  ControlOutlined,
  FilePdfOutlined,
  SettingOutlined,
  LogoutOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined,
} from '@ant-design/icons';
import { Link, useLocation, useNavigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const { Header, Sider, Content } = Layout;

const AdminLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuthStore();
  const location = useLocation();
  const navigate = useNavigate();
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const menuItems = [
    { key: '/', icon: <DashboardOutlined />, label: <Link to="/">Dashboard</Link> },
    { key: '/live-map', icon: <EnvironmentOutlined />, label: <Link to="/live-map">Live Map</Link> },
    { key: '/employees', icon: <UserOutlined />, label: <Link to="/employees">Employees</Link> },
    { key: '/history', icon: <HistoryOutlined />, label: <Link to="/history">Location History</Link> },
    { key: '/calls', icon: <PhoneOutlined />, label: <Link to="/calls">Call Logs</Link> },
    { key: '/alerts', icon: <AlertOutlined />, label: <Link to="/alerts">Alerts</Link> },
    { key: '/sim-alerts', icon: <SafetyCertificateOutlined />, label: <Link to="/sim-alerts">SIM Alerts</Link> },
    { key: '/remote-control', icon: <ControlOutlined />, label: <Link to="/remote-control">Remote Control</Link> },
    { key: '/reports', icon: <FilePdfOutlined />, label: <Link to="/reports">Reports</Link> },
    { key: '/settings', icon: <SettingOutlined />, label: <Link to="/settings">Settings</Link> },
  ].filter(item => {
    if (!user) return false;
    // Basic RBAC filtering
    if (user.role === 'SECURITY_OFFICER') {
        return ['/alerts', '/sim-alerts'].includes(item.key as string);
    }
    if (user.role === 'HR_MANAGER') {
        return !['/remote-control'].includes(item.key as string);
    }
    return true;
  });

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed}>
        <div className="demo-logo-vertical" style={{ height: 32, margin: 16, background: 'rgba(255, 255, 255, 0.2)', borderRadius: 6, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontWeight: 'bold' }}>
           {!collapsed && "MONITOR ADMIN"}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          defaultSelectedKeys={[location.pathname]}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: colorBgContainer, display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 24 }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{ fontSize: 16, width: 64, height: 64 }}
          />
          <Space>
            <Dropdown menu={{ items: [{ key: 'logout', label: 'Logout', icon: <LogoutOutlined />, onClick: handleLogout }] }}>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} />
                <span>{user?.username} ({user?.role})</span>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content
          style={{
            margin: '24dp 16dp',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            overflow: 'initial'
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default AdminLayout;
