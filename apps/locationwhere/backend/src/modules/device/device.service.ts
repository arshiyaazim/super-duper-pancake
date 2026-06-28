import prisma from '../../config/database';
import admin, { isFirebaseAdminInitialized } from '../../config/firebase';

export const registerDevice = async (employeeId: string, deviceData: any) => {
  const [device] = await prisma.$transaction([
    prisma.deviceInfo.upsert({
      where: { employeeId },
      update: {
        ...deviceData,
        lastSeen: new Date(),
        updatedAt: new Date()
      },
      create: {
        employeeId,
        ...deviceData,
        lastSeen: new Date()
      }
    }),
    prisma.employee.update({
      where: { id: employeeId },
      data: {
        registrationStatus: 'REGISTERED',
        ...(deviceData.fcmToken ? { fcmToken: deviceData.fcmToken } : {})
      }
    })
  ]);

  return device;
};

export const getPendingCommands = async (employeeId: string) => {
  return prisma.remoteCommand.findMany({
    where: {
      employeeId,
      status: { in: ['PENDING', 'SENT'] }
    }
  });
};

export const markExecuted = async (commandId: string) => {
  return prisma.remoteCommand.update({
    where: { id: commandId },
    data: {
      status: 'EXECUTED',
      executedAt: new Date()
    }
  });
};

export const sendCommand = async (employeeId: string, adminId: string, type: string, payload?: any) => {
  const employee = await prisma.employee.findUnique({
    where: { id: employeeId },
    include: { deviceInfo: true }
  });

  const fcmToken = employee?.deviceInfo?.fcmToken || employee?.fcmToken;
  if (!employee || !fcmToken) {
    throw new Error('Employee or device FCM token not found');
  }
  if (!isFirebaseAdminInitialized()) {
    throw new Error('Firebase Admin SDK is not initialized');
  }

  const command = await prisma.remoteCommand.create({
    data: {
      employeeId,
      adminId,
      commandType: type as any,
      commandPayload: payload,
      status: 'SENT',
      sentAt: new Date()
    }
  });

  await admin.messaging().send({
    token: fcmToken,
    data: {
      type: 'REMOTE_COMMAND',
      command: type,
      commandId: command.id,
      payload: payload ? JSON.stringify(payload) : ''
    },
    android: { priority: 'high' }
  });

  return command;
};

export const sendRemoteLock = async (employeeId: string, adminId: string) => {
    return sendCommand(employeeId, adminId, 'LOCK');
};
