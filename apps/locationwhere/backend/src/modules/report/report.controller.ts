import { Request, Response } from 'express';
import * as reportService from './report.service';

export const getDaily = async (req: Request, res: Response) => {
  try {
    const { date, employeeId } = req.query;
    const report = await reportService.getDailyReport(date as string, employeeId as string);
    res.json({ success: true, data: report });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};

export const downloadPdf = async (req: Request, res: Response) => {
  try {
    const pdfBuffer = await reportService.generatePdfReport(req.body);
    res.set({
      'Content-Type': 'application/pdf',
      'Content-Disposition': 'attachment; filename=report.pdf',
      'Content-Length': pdfBuffer.length,
    });
    res.send(pdfBuffer);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
};
