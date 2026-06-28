import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import dotenv from 'dotenv';

dotenv.config();

import authRoutes from './modules/auth/auth.routes';
import employeeRoutes from './modules/employee/employee.routes';
import gatewayRoutes from './modules/gateway/gateway.routes';
import locationRoutes from './modules/location/location.routes';
import simRoutes from './modules/sim/sim.routes';
import callRoutes from './modules/call/call.routes';
import deviceRoutes from './modules/device/device.routes';
import alertRoutes from './modules/alert/alert.routes';
import reportRoutes from './modules/report/report.routes';

import { connectRedis } from './config/redis';
import logger from './utils/logger';
import { apiLimiter } from './middleware/rateLimit.middleware';
import { isFirebaseAdminInitialized } from './config/firebase';

const app = express();

app.set('trust proxy', 1);

app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(apiLimiter);

app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/employees', employeeRoutes);
app.use('/api/v1/gateway', gatewayRoutes);
app.use('/api/v1/location', locationRoutes);
app.use('/api/v1/sim', simRoutes);
app.use('/api/v1/calls', callRoutes);
app.use('/api/v1/device', deviceRoutes);
app.use('/api/v1/alerts', alertRoutes);
app.use('/api/v1/reports', reportRoutes);

app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date(),
    firebaseAdminInitialized: isFirebaseAdminInitialized()
  });
});

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  logger.error(err.stack);
  res.status(err.status || 500).json({
    success: false,
    error: err.message || 'Internal Server Error',
    code: err.code || 'INTERNAL_ERROR'
  });
});

const PORT = parseInt(process.env.PORT || '3000', 10);
const HOST = process.env.HOST || '0.0.0.0';

const startServer = async () => {
  try {
    await connectRedis();
    app.listen(PORT, HOST, () => {
      logger.info(`Server running on port ${PORT} host ${HOST}`);
    });
  } catch (error) {
    logger.error('Failed to start server', error);
    process.exit(1);
  }
};

startServer();

export default app;
