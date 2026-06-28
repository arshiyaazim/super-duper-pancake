import { Router } from 'express';
import * as alertController from './alert.controller';
import { authAdmin } from '../../middleware/auth.middleware';

const router = Router();

router.get('/', authAdmin, alertController.listAlerts);
router.put('/:id/read', authAdmin, alertController.readAlert);

export default router;
