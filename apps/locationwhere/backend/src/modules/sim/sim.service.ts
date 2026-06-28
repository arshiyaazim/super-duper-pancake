import prisma from '../../config/database';
import { Severity } from '@prisma/client';

export const createSimAlert = async (employeeId: string, alertData: any) => {
  const { previousSim, newSim, previousIMSI, newIMSI, deviceInfo } = alertData;

  const simLog = await prisma.simChangeLog.create({
    data: {
      employeeId,
      deviceId: deviceInfo.deviceId || 'unknown',
      previousSimNumber: previousSim,
      newSimNumber: newSim,
      previousIMSI,
      newIMSI,
      deviceModel: deviceInfo.deviceModel,
      androidVersion: deviceInfo.androidVersion,
      status: 'PENDING'
    }
  });

  // Also create a high severity alert
  await prisma.alert.create({
    data: {
      employeeId,
      alertType: 'SIM_CHANGE',
      severity: 'CRITICAL',
      message: `SIM change detected! New SIM: ${newSim}`
    }
  });

  return simLog;
};
