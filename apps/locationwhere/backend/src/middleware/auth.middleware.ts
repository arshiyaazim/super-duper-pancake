import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export interface AuthRequest extends Request {
  user?: any;
}

export const authenticate = (secret: string) => {
  return (req: AuthRequest, res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return res.status(401).json({ success: false, error: 'Unauthorized', code: 'UNAUTHORIZED' });
    }

    const token = authHeader.split(' ')[1];
    try {
      const decoded = jwt.verify(token, secret);
      req.user = decoded;
      next();
    } catch (error) {
      return res.status(401).json({ success: false, error: 'Token expired or invalid', code: 'TOKEN_INVALID' });
    }
  };
};

export const authAdmin = authenticate(process.env.JWT_ACCESS_SECRET || 'access_secret');
export const authEmployee = authenticate(process.env.JWT_ACCESS_SECRET || 'access_secret'); // Can use different secret if needed
