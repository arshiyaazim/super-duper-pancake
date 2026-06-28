import { Router, Request, Response } from 'express';
import * as employeeService from '../employee/employee.service';

const router = Router();

const getGatewaySecret = (req: Request) => {
  const headerSecret = req.headers['x-gateway-secret'];
  if (typeof headerSecret === 'string' && headerSecret.trim()) {
    return headerSecret.trim();
  }

  if (Array.isArray(headerSecret) && headerSecret[0]?.trim()) {
    return headerSecret[0].trim();
  }

  if (typeof req.body?.secret === 'string' && req.body.secret.trim()) {
    return req.body.secret.trim();
  }

  return '';
};

const ensureAuthorized = (req: Request, res: Response) => {
  const expectedSecret = process.env.SMS_GATEWAY_SECRET?.trim();
  const secret = getGatewaySecret(req);

  if (!expectedSecret || !secret || secret !== expectedSecret) {
    res.status(401).json({ success: false, error: 'Unauthorized' });
    return false;
  }

  return true;
};

// smsgateway contract: POST {backend}/api/v1/gateway/test with body { secret }
router.post('/test', (req: Request, res: Response) => {
  if (!ensureAuthorized(req, res)) {
    return;
  }

  res.status(200).json({
    success: true,
    status: 'ok',
    timestamp: new Date().toISOString()
  });
});

// Backward-compatible route (kept for existing clients).
router.get('/test', (req: Request, res: Response) => {
  if (!ensureAuthorized(req, res)) {
    return;
  }

  res.json({ success: true, status: 'ok', timestamp: new Date().toISOString() });
});

// smsgateway contract: POST {backend}/api/v1/gateway/sms
// Body example: { secret, from, message }
router.post('/sms', async (req: Request, res: Response) => {
  if (!ensureAuthorized(req, res)) {
    return;
  }

  const sender = req.body?.from || req.body?.sender || req.body?.msisdn || '';
  const message = req.body?.message || req.body?.body || req.body?.text || req.body?.sms || '';
  const recipient = req.body?.recipient || req.body?.to || process.env.ADMIN_ONBOARDING_PHONE || '';

  if (!sender || !message) {
    res.status(400).json({
      success: false,
      status: 'error',
      error: 'Missing sender or message'
    });
    return;
  }

  try {
    const result = await employeeService.processSmsOnboarding({
      sender,
      recipient,
      body: message
    });

    if (result.status === 'created') {
      res.status(200).json({
        success: true,
        status: 'success',
        employeeCode: result.employeeCode,
        replyTo: result.replyTo,
        replyMessage: result.replyMessage
      });
      return;
    }

    if (result.status === 'duplicate') {
      res.status(200).json({
        success: true,
        status: 'duplicate',
        employeeCode: result.employeeCode,
        replyTo: result.replyTo,
        replyMessage: result.replyMessage
      });
      return;
    }

    res.status(200).json({
      success: false,
      status: result.status,
      replyTo: 'replyTo' in result ? result.replyTo : undefined,
      replyMessage: 'replyMessage' in result ? result.replyMessage : undefined
    });
  } catch (error: any) {
    res.status(200).json({
      success: false,
      status: 'error',
      error: error?.message || 'Gateway processing failed'
    });
  }
});

router.get('/status', (req: Request, res: Response) => {
  if (!ensureAuthorized(req, res)) {
    return;
  }

  res.json({
    success: true,
    status: 'ok',
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'unknown',
    checks: {
      jwt_access_secret: !!process.env.JWT_ACCESS_SECRET,
      jwt_refresh_secret: !!process.env.JWT_REFRESH_SECRET,
      sms_gateway_secret: !!process.env.SMS_GATEWAY_SECRET,
      admin_onboarding_phone: !!process.env.ADMIN_ONBOARDING_PHONE,
      apk_download_url: !!process.env.APK_DOWNLOAD_URL,
      database_url: !!process.env.DATABASE_URL,
    }
  });
});

export default router;
