import { Request, Response } from 'express';
import * as employeeService from './employee.service';
import logger from '../../utils/logger';
import { maskPhoneNumber, normalizePhoneNumber } from './employee.utils';

const safeMaskPhone = (value: string) => {
  try {
    return value ? maskPhoneNumber(value) : 'unknown';
  } catch {
    return 'unknown';
  }
};

const safeNormalizePhone = (value: string) => {
  try {
    return value ? normalizePhoneNumber(value) : 'unknown';
  } catch {
    return 'unknown';
  }
};

export const getEmployees = async (req: Request, res: Response) => {
  try {
    const result = await employeeService.listEmployees(req.query);
    res.json({ success: true, ...result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const createEmployee = async (req: Request, res: Response) => {
  try {
    const result = await employeeService.createEmployee(req.body);
    res.status(201).json({ success: true, data: result });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};

export const updateEmployee = async (req: Request, res: Response) => {
  try {
    const result = await employeeService.updateEmployee(req.params.id, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};

export const deleteEmployee = async (req: Request, res: Response) => {
  try {
    const result = await employeeService.softDeleteEmployee(req.params.id);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};

export const changePassword = async (req: Request, res: Response) => {
  try {
    const result = await employeeService.changePassword(req.params.id, req.body.password);
    res.json({ success: true, data: result, message: 'Password updated successfully' });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};

export const regeneratePassword = async (req: Request, res: Response) => {
  try {
    const result = await employeeService.regeneratePassword(req.params.id);
    res.json({ success: true, data: result, message: 'Password regenerated successfully' });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};

export const smsOnboardingHandler = async (req: Request, res: Response) => {
  const expectedSecret = process.env.SMS_GATEWAY_SECRET?.trim();
  if (!expectedSecret) {
    logger.error('SMS_GATEWAY_SECRET is not configured');
    return res.status(500).json({ success: false, error: 'Gateway not configured' });
  }
  const receivedSecret = req.headers['x-gateway-secret'];
  if (!receivedSecret || receivedSecret !== expectedSecret) {
    return res.status(401).json({ success: false, error: 'Unauthorized' });
  }

  const payload = {
    sender: req.body.sender || req.body.from || req.body.msisdn || '',
    recipient: req.body.recipient || req.body.to || req.body.destination || '',
    body: req.body.body || req.body.message || req.body.text || req.body.sms || ''
  };

  try {
    const result = await employeeService.processSmsOnboarding(payload);
    logger.info('sms_onboarding_processed', {
      event: 'sms_onboarding',
      sender_masked: safeMaskPhone(payload.sender),
      recipient: safeNormalizePhone(payload.recipient),
      outcome: result.status,
      employeeCode: 'employeeCode' in result ? result.employeeCode : undefined
    });
    res.status(200).json({ success: true, data: result });
  } catch (error: any) {
    logger.error('sms_onboarding_failed', {
      event: 'sms_onboarding',
      sender_masked: safeMaskPhone(payload.sender),
      recipient: safeNormalizePhone(payload.recipient),
      outcome: 'error',
      error: error.message
    });
    res.status(200).json({ success: false, data: { status: 'error' }, error: error.message });
  }
};

export const submitConsent = async (req: any, res: Response) => {
  try {
    const id = req.params.id || req.user.id;
    const result = await employeeService.updateConsent(id, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(400).json({ success: false, error: error.message });
  }
};
