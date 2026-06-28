import { Request, Response } from 'express';
import * as locationService from './location.service';
import { AuthRequest } from '../../middleware/auth.middleware';

export const updateLocation = async (req: AuthRequest, res: Response) => {
  try {
    const employeeId = req.user.id;
    const result = await locationService.logLocation(employeeId, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getLive = async (req: AuthRequest, res: Response) => {
  try {
    const branchId = req.user.role === 'SUPER_ADMIN' ? undefined : req.user.branchId;
    const employees = await locationService.getLiveLocations(branchId);
    res.json({ success: true, data: { employees } });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getEmployeeHistory = async (req: Request, res: Response) => {
  try {
    const { employeeId } = req.params;
    const { from, to, limit } = req.query;
    const parsedLimit = typeof limit === 'string' ? Number.parseInt(limit, 10) : undefined;

    if (parsedLimit !== undefined && (!Number.isFinite(parsedLimit) || parsedLimit <= 0)) {
      throw new Error('Invalid limit parameter');
    }

    const now = new Date();
    const defaultFrom = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const fromDate = typeof from === 'string' && from.trim() ? new Date(from) : defaultFrom;
    const toDate = typeof to === 'string' && to.trim() ? new Date(to) : now;

    if (Number.isNaN(fromDate.getTime()) || Number.isNaN(toDate.getTime())) {
      throw new Error('Invalid date range');
    }

    const routes = await locationService.getHistory(
      employeeId,
      fromDate,
      toDate,
      parsedLimit
    );
    res.json({ success: true, data: { routes } });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};

export const listGeofences = async (req: Request, res: Response) => {
  try {
    const geofences = await locationService.getGeofences();
    res.json({ success: true, data: geofences });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const breachGeofence = async (req: AuthRequest, res: Response) => {
  try {
    const employeeId = req.user.id;
    const result = await locationService.reportBreach(employeeId, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};
