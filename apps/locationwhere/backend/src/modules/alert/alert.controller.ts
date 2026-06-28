import { Request, Response } from 'express';
import * as alertService from './alert.service';

export const listAlerts = async (req: Request, res: Response) => {
  try {
    const alerts = await alertService.getAlerts(req.query);
    res.json({ success: true, data: alerts });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const readAlert = async (req: any, res: Response) => {
  try {
    const { id } = req.params;
    const adminId = req.user.id;
    const result = await alertService.markAsRead(id, adminId);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};
