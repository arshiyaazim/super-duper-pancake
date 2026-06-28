import { Router } from 'express';
import * as deviceController from './device.controller';
import { authAdmin, authEmployee } from '../../middleware/auth.middleware';
import { requireRole } from '../../middleware/rbac.middleware';

const router = Router();

router.post('/register', authEmployee, deviceController.register);
router.get('/commands/pending', authEmployee, deviceController.pendingCommands);
router.post('/commands/executed', authEmployee, deviceController.executed);
router.post('/commands', authAdmin, requireRole('SUPER_ADMIN', 'SECURITY_OFFICER'), deviceController.postCommand);
router.post('/remote-lock', authAdmin, requireRole('SUPER_ADMIN', 'SECURITY_OFFICER'), deviceController.lockDevice);

export default router;
