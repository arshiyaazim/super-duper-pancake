import { createClient } from 'redis';
import logger from '../utils/logger';

const redisClient = createClient({
  url: process.env.REDIS_URL || 'redis://localhost:6379'
});

const isRedisOptional = process.env.REDIS_OPTIONAL === 'true';

redisClient.on('error', (err) => logger.error('Redis Client Error', err));

export const connectRedis = async () => {
  if (!redisClient.isOpen) {
    try {
      await redisClient.connect();
      logger.info('Connected to Redis');
    } catch (error) {
      if (isRedisOptional) {
        logger.warn('Redis unavailable, continuing without cache');
        return;
      }
      throw error;
    }
  }
};

export const isRedisReady = () => redisClient.isOpen;

export default redisClient;
