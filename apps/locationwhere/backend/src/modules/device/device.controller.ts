import { Response } from 'express';
import { AuthRequest } from '../../middleware/auth.middleware';
import * as deviceService from './device.service';

export const register = async (req: AuthRequest, res: Response) => {
  try {
    const employeeId = req.user.id;
    const result = await deviceService.registerDevice(employeeId, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const pendingCommands = async (req: AuthRequest, res: Response) => {
  try {
    const employeeId = req.user.id;
    const commands = await deviceService.getPendingCommands(employeeId);
    res.json({ success: true, data: commands });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const executed = async (req: AuthRequest, res: Response) => {
  try {
    const { commandId } = req.body;
    const result = await deviceService.markExecuted(commandId);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const postCommand = async (req: AuthRequest, res: Response) => {
  try {
    const { employeeId, commandType, payload } = req.body;
    const adminId = req.user.id;
    const result = await deviceService.sendCommand(employeeId, adminId, commandType, payload);
    res.json({ success: true, data: result, message: `Command ${commandType} sent` });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const lockDevice = async (req: AuthRequest, res: Response) => {
  try {
    const { employeeId } = req.body;
    const adminId = req.user.id;
    const result = await deviceService.sendRemoteLock(employeeId, adminId);
    res.json({ success: true, data: result, message: 'Lock command sent' });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};
