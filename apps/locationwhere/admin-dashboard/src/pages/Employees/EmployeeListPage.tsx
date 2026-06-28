import React, { useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';

const { Text, Paragraph } = Typography;

type EmployeeModalMode = 'create' | 'edit' | 'password';

const EmployeeListPage: React.FC = () => {
  const [employeeModalMode, setEmployeeModalMode] = useState<EmployeeModalMode>('create');
  const [modalOpen, setModalOpen] = useState(false);
  const [activeEmployee, setActiveEmployee] = useState<any>(null);
  const [searchValue, setSearchValue] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: employees = [], isLoading } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await api.get('/employees');
      return response.data.data;
    }
  });

  const refreshEmployees = () =>
    queryClient.invalidateQueries({ queryKey: ['employees'] });

  const showCredentialModal = (title: string, payload: any) => {
    if (!payload?.generatedPassword) return;

    Modal.info({
      title,
      width: 520,
      content: (
        <div style={{ marginTop: 12 }}>
          <Paragraph copyable>{`Employee ID: ${payload.employee?.employeeCode}`}</Paragraph>
          <Paragraph copyable>{`Password: ${payload.generatedPassword}`}</Paragraph>
          <Text type="secondary">This password is shown only once.</Text>
        </div>
      )
    });
  };

  const createMutation = useMutation({
    mutationFn: (values: any) => api.post('/employees', values),
    onSuccess: ({ data }) => {
      message.success('Employee added successfully');
      setModalOpen(false);
      form.resetFields();
      showCredentialModal('Employee credentials', data.data);
      refreshEmployees();
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: string; values: any }) => api.put(`/employees/${id}`, values),
    onSuccess: () => {
      message.success('Employee updated successfully');
      setModalOpen(false);
      setActiveEmployee(null);
      form.resetFields();
      refreshEmployees();
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/employees/${id}`),
    onSuccess: () => {
      message.success('Employee deactivated successfully');
      refreshEmployees();
    }
  });

  const changePasswordMutation = useMutation({
    mutationFn: ({ id, password }: { id: string; password: string }) =>
      api.post(`/employees/${id}/change-password`, { password }),
    onSuccess: () => {
      message.success('Password updated successfully');
      setModalOpen(false);
      setActiveEmployee(null);
      form.resetFields();
    }
  });

  const regeneratePasswordMutation = useMutation({
    mutationFn: (id: string) => api.post(`/employees/${id}/regenerate-password`),
    onSuccess: ({ data }) => {
      message.success('Password regenerated successfully');
      showCredentialModal('Regenerated credentials', data.data);
      refreshEmployees();
    }
  });

  const openCreateModal = () => {
    setEmployeeModalMode('create');
    setActiveEmployee(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEditModal = (employee: any) => {
    setEmployeeModalMode('edit');
    setActiveEmployee(employee);
    form.setFieldsValue({
      fullName: employee.fullName,
      email: employee.email || undefined,
      phone: employee.phone
    });
    setModalOpen(true);
  };

  const openPasswordModal = (employee: any) => {
    setEmployeeModalMode('password');
    setActiveEmployee(employee);
    form.resetFields();
    setModalOpen(true);
  };

  const handleSubmit = (values: any) => {
    if (employeeModalMode === 'create') {
      createMutation.mutate(values);
      return;
    }

    if (employeeModalMode === 'edit' && activeEmployee) {
      updateMutation.mutate({
        id: activeEmployee.id,
        values
      });
      return;
    }

    if (employeeModalMode === 'password' && activeEmployee) {
      changePasswordMutation.mutate({
        id: activeEmployee.id,
        password: values.password
      });
    }
  };

  const filteredEmployees = employees.filter((employee: any) => {
    const searchMatch =
      !searchValue ||
      employee.fullName?.toLowerCase().includes(searchValue.toLowerCase()) ||
      employee.employeeCode?.toLowerCase().includes(searchValue.toLowerCase()) ||
      employee.phone?.includes(searchValue);
    const statusMatch = !statusFilter || employee.registrationStatus === statusFilter;
    return searchMatch && statusMatch;
  });

  const columns = [
    { title: 'Code', dataIndex: 'employeeCode', key: 'code' },
    { title: 'Name', dataIndex: 'fullName', key: 'name' },
    { title: 'Phone', dataIndex: 'phone', key: 'phone' },
    {
      title: 'Status',
      dataIndex: 'registrationStatus',
      key: 'registrationStatus',
      render: (status: string) => (
        <Tag color={status === 'REGISTERED' ? 'green' : 'orange'}>
          {status === 'REGISTERED' ? 'Registered' : 'Unregistered'}
        </Tag>
      )
    },
    {
      title: 'Device',
      key: 'device',
      render: (_: any, employee: any) =>
        employee.registrationStatus === 'REGISTERED' && employee.deviceInfo?.deviceName
          ? employee.deviceInfo.deviceName
          : '-'
    },
    {
      title: 'Active',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active: boolean) => (
        <Tag color={active ? 'blue' : 'red'}>{active ? 'Active' : 'Inactive'}</Tag>
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, employee: any) => (
        <Space wrap>
          {employee.deviceInfo ? (
            <Button
              icon={<EyeOutlined />}
              onClick={() => navigate(`/live-map?employeeId=${employee.id}`)}
            >
              Show Map
            </Button>
          ) : null}
          <Button icon={<EditOutlined />} onClick={() => openEditModal(employee)}>
            Edit
          </Button>
          <Button icon={<KeyOutlined />} onClick={() => openPasswordModal(employee)}>
            Change Password
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => regeneratePasswordMutation.mutate(employee.id)}
          >
            Regenerate Password
          </Button>
          <Popconfirm
            title="Deactivate employee?"
            description="This will archive the employee account without removing device or location history."
            onConfirm={() => deleteMutation.mutate(employee.id)}
            okText="Deactivate"
            cancelText="Cancel"
          >
            <Button danger icon={<DeleteOutlined />}>
              Deactivate
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Card
        title="Employee Directory"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
            Add Employee
          </Button>
        }
      >
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            placeholder="Search name, code, or phone"
            allowClear
            value={searchValue}
            onChange={(event) => setSearchValue(event.target.value)}
            style={{ width: 260 }}
          />
          <Select
            placeholder="Filter by registration"
            allowClear
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 220 }}
          >
            <Select.Option value="REGISTERED">Registered</Select.Option>
            <Select.Option value="UNREGISTERED">Unregistered</Select.Option>
          </Select>
        </Space>

        <Table columns={columns} dataSource={filteredEmployees} loading={isLoading} rowKey="id" />
      </Card>

      <Modal
        title={
          employeeModalMode === 'create'
            ? 'Add Employee'
            : employeeModalMode === 'edit'
              ? 'Edit Employee'
              : `Change Password: ${activeEmployee?.employeeCode || ''}`
        }
        open={modalOpen}
        onOk={() => form.submit()}
        onCancel={() => {
          setModalOpen(false);
          setActiveEmployee(null);
          form.resetFields();
        }}
        okText={
          employeeModalMode === 'create'
            ? 'Create'
            : employeeModalMode === 'edit'
              ? 'Save'
              : 'Update Password'
        }
        confirmLoading={
          createMutation.isPending || updateMutation.isPending || changePasswordMutation.isPending
        }
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          {employeeModalMode !== 'password' ? (
            <>
              <Form.Item name="fullName" label="Full Name" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="phone" label="Phone" rules={[{ required: true }]}>
                <Input placeholder="018xxxxxxxx" />
              </Form.Item>
              <Form.Item name="email" label="Email">
                <Input placeholder="Optional" />
              </Form.Item>
              {employeeModalMode === 'create' ? (
                <Form.Item
                  name="password"
                  label="Password"
                  extra="Leave blank to auto-generate a one-time password."
                >
                  <Input.Password />
                </Form.Item>
              ) : (
                <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  Employee code: {activeEmployee?.employeeCode}
                </Paragraph>
              )}
            </>
          ) : (
            <Form.Item
              name="password"
              label="New Password"
              rules={[{ required: true }, { min: 6, message: 'Minimum 6 characters' }]}
            >
              <Input.Password />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default EmployeeListPage;
