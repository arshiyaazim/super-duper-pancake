import React, { useState } from 'react';
import { Card, Select, Button, Space, Row, Col, Input, Typography, Modal, message } from 'antd';
import { LockOutlined, UnlockOutlined, NotificationOutlined, MessageOutlined, WarningOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';

const { Text } = Typography;

const RemoteControlPage: React.FC = () => {
  const [selectedEmployee, setSelectedEmployee] = useState<string | null>(null);
  const [msg, setMsg] = useState('');

  const { data: employees } = useQuery({
    queryKey: ['employees-short'],
    queryFn: async () => {
      const response = await api.get('/employees');
      return response.data.data;
    }
  });

  const sendCommand = async (type: string, payload?: any) => {
    if (!selectedEmployee) {
        message.warning('Please select an employee first');
        return;
    }
    try {
        await api.post('/device/commands', {
            employeeId: selectedEmployee,
            commandType: type,
            payload
        });
        message.success(`Command ${type} sent successfully`);
    } catch (e) {
        message.error('Failed to send command');
    }
  };

  const confirmWipe = () => {
    Modal.confirm({
        title: 'CRITICAL ACTION: WIPE DATA',
        content: 'This will factory reset the device and erase all data. This action CANNOT be undone. Are you absolutely sure?',
        okText: 'YES, WIPE DATA',
        okType: 'danger',
        onOk: () => sendCommand('WIPE')
    });
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card title="Remote Device Control">
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div>
                <Text strong>Select Employee Device:</Text>
                <Select
                    style={{ width: '100%', marginTop: 8 }}
                    placeholder="Choose an employee..."
                    onChange={(val) => setSelectedEmployee(val)}
                    showSearch
                    optionFilterProp="children"
                >
                    {employees?.map((emp: any) => (
                        <Select.Option key={emp.id} value={emp.id}>{emp.fullName} ({emp.employeeCode})</Select.Option>
                    ))}
                </Select>
            </div>

            <Row gutter={[16, 16]}>
                <Col span={12}>
                    <Button
                        block
                        size="large"
                        icon={<LockOutlined />}
                        type="primary"
                        danger
                        onClick={() => sendCommand('LOCK')}
                    >
                        Lock Device
                    </Button>
                </Col>
                <Col span={12}>
                    <Button
                        block
                        size="large"
                        icon={<UnlockOutlined />}
                        onClick={() => sendCommand('UNLOCK')}
                    >
                        Unlock Device
                    </Button>
                </Col>
                <Col span={12}>
                    <Button
                        block
                        size="large"
                        icon={<NotificationOutlined />}
                        onClick={() => sendCommand('SIREN')}
                    >
                        Play Siren (30s)
                    </Button>
                </Col>
                <Col span={12}>
                    <Button
                        block
                        size="large"
                        danger
                        icon={<WarningOutlined />}
                        onClick={confirmWipe}
                    >
                        Wipe Device
                    </Button>
                </Col>
            </Row>

            <Card size="small" title="Send Message to Device">
                <Space.Compact style={{ width: '100%' }}>
                    <Input
                        prefix={<MessageOutlined />}
                        placeholder="Type a message..."
                        value={msg}
                        onChange={(e) => setMsg(e.target.value)}
                    />
                    <Button type="primary" onClick={() => sendCommand('MESSAGE', { text: msg })}>Send</Button>
                </Space.Compact>
            </Card>
        </Space>
      </Card>

      <Card title="Command History" style={{ marginTop: 24 }}>
          <Text type="secondary">Recent commands sent to devices will appear here.</Text>
      </Card>
    </div>
  );
};

export default RemoteControlPage;
