import prisma from '../../config/database';

export const getAlerts = async (filters: any) => {
  const { severity, isRead, page = 1 } = filters;
  const skip = (Number(page) - 1) * 20;

  const where: any = {};
  if (severity) where.severity = severity;
  if (isRead !== undefined) where.isRead = isRead === 'true';

  return prisma.alert.findMany({
    where,
    skip,
    take: 20,
    include: { employee: true },
    orderBy: { createdAt: 'desc' }
  });
};

export const markAsRead = async (id: string, adminId: string) => {
  return prisma.alert.update({
    where: { id },
    data: { isRead: true, readById: adminId }
  });
};
