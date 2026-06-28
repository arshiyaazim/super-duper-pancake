import { Request, Response } from 'express';
import * as callService from './call.service';
import prisma from '../../config/database';

export const logCall = async (req: any, res: Response) => {
  try {
    const employeeId = req.user.id;
    const result = await callService.logCall(employeeId, req.body);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const uploadRecording = async (req: Request, res: Response) => {
  try {
    const { callLogId, checksum } = req.body;
    if (!req.file) throw new Error('No file uploaded');
    if (!checksum) throw new Error('Recording checksum is required');
    const result = await callService.uploadRecording(callLogId, req.file, checksum);
    res.json({ success: true, data: result });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getCalls = async (req: Request, res: Response) => {
  try {
    const { employeeId, from, to, page = 1 } = req.query;
    const skip = (Number(page) - 1) * 20;
    const where: any = {};
    if (employeeId) where.employeeId = employeeId;
    if (from && to) {
      where.createdAt = { gte: new Date(from as string), lte: new Date(to as string) };
    }
    const calls = await prisma.callLog.findMany({
      where,
      skip,
      take: 20,
      orderBy: { createdAt: 'desc' }
    });
    res.json({ success: true, data: calls });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const getRecording = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const url = await callService.getRecordingUrl(id);
    res.json({ success: true, data: { url } });
  } catch (error: any) {
    res.status(404).json({ success: false, error: error.message });
  }
};
