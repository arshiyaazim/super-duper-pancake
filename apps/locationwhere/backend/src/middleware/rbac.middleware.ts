import { Response, NextFunction } from 'express';
import { AuthRequest } from './auth.middleware';
import { Role } from '@prisma/client';

export const requireRole = (...roles: Role[]) => {
  return (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        error: 'Forbidden: You do not have permission',
        code: 'FORBIDDEN'
      });
    }
    next();
  };
};

export const branchFilter = (req: AuthRequest, res: Response, next: NextFunction) => {
  if (req.user.role !== 'SUPER_ADMIN' && req.user.branchId) {
    // Inject branch filter into request for services to use
    req.query.branchId = req.user.branchId;
  }
  next();
};
