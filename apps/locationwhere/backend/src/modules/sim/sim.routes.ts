import { Router } from 'express';
import * as simController from './sim.controller';
import { authAdmin, authEmployee } from '../../middleware/auth.middleware';

const router = Router();

router.post('/change-alert', authEmployee, simController.createAlert);
router.get('/alerts', authAdmin, simController.getAlerts);
router.put('/alerts/:id/resolve', authAdmin, simController.resolveAlert);

export default router;
