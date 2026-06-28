import { Router } from 'express';
import * as authController from './auth.controller';
import { validate } from '../../middleware/validate.middleware';
import { loginLimiter } from '../../middleware/rateLimit.middleware';
import { loginSchema, mobileLoginSchema, refreshSchema } from './auth.schema';

const router = Router();

router.post('/login', loginLimiter, validate(loginSchema), authController.login);
router.post('/mobile/login', loginLimiter, validate(mobileLoginSchema), authController.initiateMobileLogin);
router.post('/refresh', loginLimiter, validate(refreshSchema), authController.refresh);
router.post('/verify-otp', loginLimiter, authController.verifyOTP);
router.post('/logout', authController.logout);

export default router;
