import { Request, Response } from 'express';
import * as authService from './auth.service';

export const login = async (req: Request, res: Response) => {
  try {
    const { username, password } = req.body;
    const result = await authService.adminLogin(username, password);
    res.json({ success: true, data: result, message: 'Login successful' });
  } catch (error: any) {
    res.status(401).json({ success: false, error: error.message, code: 'AUTH_FAILED' });
  }
};

export const initiateMobileLogin = async (req: Request, res: Response) => {
  try {
    const { employeeCode, password, deviceId, fcmToken } = req.body;
    const result = await authService.employeeMobileLogin(employeeCode, password, deviceId, fcmToken);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(401).json({ success: false, error: error.message, code: 'AUTH_FAILED' });
  }
};

export const refresh = async (req: Request, res: Response) => {
  try {
    const { refreshToken } = req.body;
    const result = await authService.refreshAccessToken(refreshToken);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(401).json({ success: false, error: error.message, code: 'TOKEN_INVALID' });
  }
};

export const verifyOTP = async (req: Request, res: Response) => {
  try {
    const { employeeCode, otp, deviceId } = req.body;
    const result = await authService.verifyOTP(employeeCode, otp, deviceId);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(401).json({ success: false, error: error.message });
  }
};

export const logout = async (req: Request, res: Response) => {
  res.json({ success: true, message: 'Logged out successfully' });
};
