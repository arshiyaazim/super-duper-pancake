import React from 'react';
import { Row, Col, Card, Statistic, Table, Tag } from 'antd';
import { UserOutlined, AlertOutlined, SafetyOutlined, TeamOutlined } from '@ant-design/icons';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';

const DashboardPage: React.FC = () => {
  const { data: stats } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: async () => {
      // In real implementation, this would be a single stats endpoint
      return {
        totalEmployees: 200,
        online: 145,
        offline: 55,
        todayAlerts: 12,
        pendingSimAlerts: 3,
      };
    }
  });

  const { data: recentAlerts } = useQuery({
    queryKey: ['recent-alerts'],
    queryFn: async () => {
        const response = await api.get('/alerts?page=1&limit=10');
        return response.data.data;
    }
  });

  const pieData = [
    { name: 'Online', value: stats?.online || 0 },
    { name: 'Offline', value: stats?.offline || 0 },
  ];
  const COLORS = ['#52c41a', '#f5222d'];

  const columns = [
    { title: 'Employee', dataIndex: ['employee', 'fullName'], key: 'name' },
    { title: 'Type', dataIndex: 'alertType', key: 'type', render: (type: string) => <Tag color="red">{type}</Tag> },
    { title: 'Severity', dataIndex: 'severity', key: 'severity' },
    { title: 'Time', dataIndex: 'createdAt', key: 'time', render: (time: string) => new Date(time).toLocaleString() },
  ];

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card bordered={false}>
            <Statistic title="Active Employees" value={stats?.online} prefix={<UserOutlined />} valueStyle={{ color: '#3f8600' }} suffix={`/ ${stats?.totalEmployees}`} />
          </Card>
        </Col>
        <Col span={6}>
          <Card bordered={false}>
            <Statistic title="Today's Alerts" value={stats?.todayAlerts} prefix={<AlertOutlined />} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card bordered={false}>
            <Statistic title="SIM Change Warnings" value={stats?.pendingSimAlerts} prefix={<SafetyOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card bordered={false}>
            <Statistic title="Offline Workers" value={stats?.offline} prefix={<TeamOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col span={16}>
          <Card title="Recent Alerts">
            <Table dataSource={recentAlerts} columns={columns} pagination={false} size="small" rowKey="id" />
          </Card>
        </Col>
        <Col span={8}>
          <Card title="Status Distribution">
            <div style={{ height: 250 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={pieData} innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                    {pieData.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div style={{ textAlign: 'center' }}>
                <Tag color="green">Online: {stats?.online}</Tag>
                <Tag color="red">Offline: {stats?.offline}</Tag>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
          <Col span={24}>
              <Card title="Alert History (Last 7 Days)">
                <div style={{ height: 300 }}>
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={[
                            { day: 'Mon', count: 4 },
                            { day: 'Tue', count: 7 },
                            { day: 'Wed', count: 2 },
                            { day: 'Thu', count: 5 },
                            { day: 'Fri', count: 9 },
                            { day: 'Sat', count: 1 },
                            { day: 'Sun', count: 3 },
                        ]}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="day" />
                            <YAxis />
                            <Tooltip />
                            <Bar dataKey="count" fill="#1890ff" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
              </Card>
          </Col>
      </Row>
    </div>
  );
};

export default DashboardPage;
