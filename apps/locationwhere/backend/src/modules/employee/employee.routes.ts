import { Router } from 'express';
import * as employeeController from './employee.controller';
import { authAdmin, authEmployee } from '../../middleware/auth.middleware';
import { requireRole, branchFilter } from '../../middleware/rbac.middleware';

const router = Router();

router.post('/onboarding/sms', employeeController.smsOnboardingHandler);
router.get('/', authAdmin, branchFilter, employeeController.getEmployees);
router.post('/', authAdmin, requireRole('SUPER_ADMIN', 'HR_MANAGER'), employeeController.createEmployee);
router.put('/:id', authAdmin, requireRole('SUPER_ADMIN', 'HR_MANAGER'), employeeController.updateEmployee);
router.delete('/:id', authAdmin, requireRole('SUPER_ADMIN', 'HR_MANAGER'), employeeController.deleteEmployee);
router.post(
  '/:id/change-password',
  authAdmin,
  requireRole('SUPER_ADMIN', 'HR_MANAGER'),
  employeeController.changePassword
);
router.post(
  '/:id/regenerate-password',
  authAdmin,
  requireRole('SUPER_ADMIN', 'HR_MANAGER'),
  employeeController.regeneratePassword
);
router.post('/:id/password', authAdmin, requireRole('SUPER_ADMIN', 'HR_MANAGER'), employeeController.changePassword);
router.post(
  '/:id/password/regenerate',
  authAdmin,
  requireRole('SUPER_ADMIN', 'HR_MANAGER'),
  employeeController.regeneratePassword
);
router.post('/consent', authEmployee, employeeController.submitConsent);
router.post('/:id/consent', authAdmin, employeeController.submitConsent);

export default router;
