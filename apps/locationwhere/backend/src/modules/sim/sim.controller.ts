import { Request, Response } from 'express';
import * as simService from './sim.service';
import prisma from '../../config/database';

export const createAlert = async (req: any, res: Response) => {
  try {
    const employeeId = req.user.id;
    const result = await simService.createSimAlert(employeeId, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getAlerts = async (req: Request, res: Response) => {
  try {
    const { status, page = 1 } = req.query;
    const skip = (Number(page) - 1) * 20;
    const alerts = await prisma.simChangeLog.findMany({
      where: status ? { status: status as any } : {},
      skip,
      take: 20,
      include: { employee: true },
      orderBy: { detectedAt: 'desc' }
    });
    res.json({ success: true, data: alerts });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const resolveAlert = async (req: any, res: Response) => {
  try {
    const { id } = req.params;
    const { status, note } = req.body;
    const result = await prisma.simChangeLog.update({
      where: { id },
      data: {
        status,
        resolvedAt: new Date(),
        resolvedById: req.user.id
      }
    });
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};
