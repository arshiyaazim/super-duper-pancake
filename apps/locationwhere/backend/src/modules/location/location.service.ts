import prisma from '../../config/database';
import redisClient, { isRedisReady } from '../../config/redis';

export const logLocation = async (employeeId: string, locationData: any) => {
  const { latitude, longitude, accuracy, batteryLevel, address } = locationData;

  const log = await prisma.locationLog.create({
    data: {
      employeeId,
      latitude,
      longitude,
      accuracy,
      batteryLevel,
      address,
      recordedAt: new Date()
    }
  });

  if (isRedisReady()) {
    const employee = await prisma.employee.findUnique({
      where: { id: employeeId },
      select: { fullName: true, employeeCode: true }
    });

    await redisClient.hSet('live_locations', employeeId, JSON.stringify({
      latitude,
      longitude,
      battery: batteryLevel,
      name: employee?.fullName,
      employeeCode: employee?.employeeCode,
      lastSeen: new Date(),
      id: employeeId,
      isOnline: true
    }));
  }

  return log;
};

export const getLiveLocations = async (branchId?: string) => {
  if (isRedisReady()) {
    const allLocations = await redisClient.hGetAll('live_locations');
    return Object.values(allLocations).map(loc => JSON.parse(loc));
  }

  const latestLogs = await prisma.locationLog.findMany({
    distinct: ['employeeId'],
    orderBy: { recordedAt: 'desc' },
    include: {
      employee: {
        select: {
          id: true,
          fullName: true,
          employeeCode: true,
          branchId: true,
          isActive: true
        }
      }
    }
  });

  return latestLogs
    .filter(log => log.employee.isActive && (!branchId || log.employee.branchId === branchId))
    .map(log => ({
      id: log.employeeId,
      name: log.employee.fullName,
      employeeCode: log.employee.employeeCode,
      latitude: log.latitude,
      longitude: log.longitude,
      battery: log.batteryLevel,
      isOnline: true,
      lastSeen: log.recordedAt
    }));
};

export const getHistory = async (employeeId: string, from: Date, to: Date, limit?: number) => {
  return prisma.locationLog.findMany({
    where: {
      employeeId,
      recordedAt: { gte: from, lte: to }
    },
    orderBy: { recordedAt: 'asc' },
    ...(limit ? { take: limit } : {})
  });
};

export const getGeofences = async () => {
  return prisma.geofence.findMany({
    where: { isActive: true }
  });
};

export const reportBreach = async (employeeId: string, data: any) => {
  return prisma.geofenceAlert.create({
    data: {
      employeeId,
      geofenceId: data.geofenceId,
      alertType: data.alertType,
      latitude: data.latitude,
      longitude: data.longitude,
      triggeredAt: new Date()
    }
  });
};
