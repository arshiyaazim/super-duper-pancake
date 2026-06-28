import rateLimit from 'express-rate-limit';
import { AuthRequest } from './auth.middleware';

export const loginLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 5,
  message: {
    success: false,
    error: 'Too many login attempts, please try again after 5 minutes',
    code: 'TOO_MANY_REQUESTS'
  },
  standardHeaders: true,
  legacyHeaders: false,
});

export const apiLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 100,
  message: {
    success: false,
    error: 'Too many requests, please try again later',
    code: 'TOO_MANY_REQUESTS'
  }
});

export const locationLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 3,
  message: {
    success: false,
    error: 'Location update rate limit exceeded',
    code: 'TOO_MANY_REQUESTS'
  },
  keyGenerator: (req) => (req as AuthRequest).user?.id || req.ip
});
