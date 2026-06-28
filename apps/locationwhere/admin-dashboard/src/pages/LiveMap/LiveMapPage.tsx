import React from 'react';
import { Layout, List, Avatar, Badge, Input, Typography } from 'antd';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMap } from 'react-leaflet';
import L from 'leaflet';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import api from '../../services/api';

const { Sider, Content } = Layout;
const { Text } = Typography;

const MapFocus: React.FC<{ latitude?: number; longitude?: number }> = ({ latitude, longitude }) => {
  const map = useMap();

  React.useEffect(() => {
    if (latitude !== undefined && longitude !== undefined) {
      map.setView([latitude, longitude], 16);
    }
  }, [latitude, longitude, map]);

  return null;
};

const LiveMapPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const selectedEmployeeId = searchParams.get('employeeId');

  const { data: employees = [] } = useQuery({
    queryKey: ['live-locations'],
    queryFn: async () => {
      const response = await api.get('/location/live');
      return response.data.data.employees;
    },
    refetchInterval: 30000
  });

  const selectedEmployee =
    employees.find((employee: any) => employee.id === selectedEmployeeId) || employees[0];

  const getIcon = (isOnline: boolean, isSelected: boolean) =>
    L.divIcon({
      className: 'custom-icon',
      html: `<div style="background-color: ${isOnline ? '#52c41a' : '#f5222d'}; width: ${isSelected ? 16 : 12}px; height: ${isSelected ? 16 : 12}px; border-radius: 50%; border: 2px solid white; box-shadow: 0 0 4px rgba(0,0,0,0.3);"></div>`
    });

  return (
    <Layout style={{ height: 'calc(100vh - 112px)', margin: -24 }}>
      <Sider width={320} theme="light" style={{ borderRight: '1px solid #f0f0f0', overflowY: 'auto' }}>
        <div style={{ padding: 16 }}>
          <Input.Search placeholder="Search employees..." allowClear />
          {selectedEmployee ? (
            <div style={{ marginTop: 12 }}>
              <Text type="secondary">Focused employee</Text>
              <div style={{ fontWeight: 600 }}>{selectedEmployee.name}</div>
            </div>
          ) : null}
        </div>
        <List
          itemLayout="horizontal"
          dataSource={employees}
          renderItem={(employee: any) => (
            <List.Item
              style={{
                cursor: 'pointer',
                padding: '12px 16px',
                background: employee.id === selectedEmployeeId ? '#f6ffed' : 'transparent'
              }}
            >
              <List.Item.Meta
                avatar={<Avatar>{employee.name?.[0] || 'E'}</Avatar>}
                title={`${employee.name} (${employee.employeeCode})`}
                description={
                  <div>
                    <Badge status={employee.isOnline ? 'success' : 'error'} text={employee.isOnline ? 'Online' : 'Offline'} />
                    <span style={{ marginLeft: 8 }}>Battery {employee.battery ?? '-'}%</span>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Sider>
      <Content>
        <MapContainer center={[23.8103, 90.4125]} zoom={13} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapFocus latitude={selectedEmployee?.latitude} longitude={selectedEmployee?.longitude} />
          {employees.map((employee: any) => (
            <Marker
              key={employee.id}
              position={[employee.latitude, employee.longitude]}
              icon={getIcon(employee.isOnline, employee.id === selectedEmployeeId)}
            >
              <Popup>
                <strong>{employee.name}</strong>
                <br />
                Employee ID: {employee.employeeCode}
                <br />
                Last Seen: {new Date(employee.lastSeen).toLocaleTimeString()}
                <br />
                Battery: {employee.battery ?? '-'}%
              </Popup>
            </Marker>
          ))}
          <Circle center={[23.8103, 90.4125]} radius={1000} pathOptions={{ color: 'red' }} />
        </MapContainer>
      </Content>
    </Layout>
  );
};

export default LiveMapPage;
