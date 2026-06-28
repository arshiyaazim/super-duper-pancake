import React, { useState } from 'react';
import { Table, Button, Space, Tag, Card, Modal, Input, message } from 'antd';
import { SafetyOutlined, LockOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';

const SimAlertsPage: React.FC = () => {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [selectedAlert, setSelectedAlert] = useState<any>(null);
  const [note, setNote] = useState('');
  const queryClient = useQueryClient();

  const { data: alerts, isLoading } = useQuery({
    queryKey: ['sim-alerts'],
    queryFn: async () => {
      const response = await api.get('/sim/alerts');
      return response.data.data;
    }
  });

  const resolveMutation = useMutation({
    mutationFn: (data: any) => api.put(`/sim/alerts/${data.id}/resolve`, { status: data.status, note: data.note }),
    onSuccess: () => {
      message.success('Alert resolved');
      setIsModalVisible(false);
      queryClient.invalidateQueries({ queryKey: ['sim-alerts'] });
    }
  });

  const columns = [
    { title: 'Employee', dataIndex: ['employee', 'fullName'], key: 'name' },
    { title: 'Old SIM', dataIndex: 'previousSimNumber', key: 'oldSim' },
    { title: 'New SIM', dataIndex: 'newSimNumber', key: 'newSim' },
    { title: 'Detected At', dataIndex: 'detectedAt', key: 'time', render: (t: string) => new Date(t).toLocaleString() },
    {
        title: 'Status',
        dataIndex: 'status',
        key: 'status',
        render: (status: string) => (
            <Tag color={status === 'PENDING' ? 'red' : 'green'}>{status}</Tag>
        )
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, record: any) => (
        record.status === 'PENDING' && (
            <Space>
                <Button
                    type="primary"
                    icon={<CheckCircleOutlined />}
                    onClick={() => { setSelectedAlert(record); setIsModalVisible(true); }}
                >
                    Resolve
                </Button>
                <Button danger icon={<LockOutlined />}>Lock Device</Button>
            </Space>
        )
      ),
    },
  ];

  return (
    <div>
      <Card title={<span><SafetyOutlined /> SIM Change Alerts</span>}>
        <Table columns={columns} dataSource={alerts} loading={isLoading} rowKey="id" />
      </Card>

      <Modal
        title="Resolve SIM Alert"
        open={isModalVisible}
        onOk={() => resolveMutation.mutate({ id: selectedAlert.id, status: 'RESOLVED', note })}
        onCancel={() => setIsModalVisible(false)}
      >
        <p>Are you sure you want to mark this as resolved?</p>
        <Input.TextArea
            placeholder="Add a resolution note..."
            value={note}
            onChange={(e) => setNote(e.target.value)}
            rows={4}
        />
        <div style={{ marginTop: 16 }}>
            <Button onClick={() => resolveMutation.mutate({ id: selectedAlert.id, status: 'FALSE_ALARM', note })}>
                Mark as False Alarm
            </Button>
        </div>
      </Modal>
    </div>
  );
};

export default SimAlertsPage;
