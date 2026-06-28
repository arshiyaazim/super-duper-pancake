import prisma from '../../config/database';
import { generateEmployeeReport } from '../../utils/pdf.generator';

export const getDailyReport = async (date: string, employeeId?: string) => {
  const targetDate = new Date(date);
  const start = new Date(targetDate.setHours(0, 0, 0, 0));
  const end = new Date(targetDate.setHours(23, 59, 59, 999));

  const where: any = { recordedAt: { gte: start, lte: end } };
  if (employeeId) where.employeeId = employeeId;

  const locations = await prisma.locationLog.findMany({ where, orderBy: { recordedAt: 'asc' } });
  const calls = await prisma.callLog.findMany({
    where: { startedAt: { gte: start, lte: end }, ...(employeeId && { employeeId }) }
  });

  return { locations, calls, date };
};

export const generatePdfReport = async (filters: any) => {
  const { employeeId, from, to } = filters;
  const employee = await prisma.employee.findUnique({ where: { id: employeeId } });
  if (!employee) throw new Error('Employee not found');

  const calls = await prisma.callLog.count({
    where: { employeeId, startedAt: { gte: new Date(from), lte: new Date(to) } }
  });

  const data = {
    fullName: employee.fullName,
    employeeCode: employee.employeeCode,
    period: `${from} to ${to}`,
    totalCalls: calls,
    geofenceBreaches: 0 // Placeholder
  };

  return generateEmployeeReport(data);
};
