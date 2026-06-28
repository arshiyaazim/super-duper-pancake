import { Router } from 'express';
import * as reportController from './report.controller';
import { authAdmin } from '../../middleware/auth.middleware';

const router = Router();

router.get('/daily', authAdmin, reportController.getDaily);
router.post('/generate-pdf', authAdmin, reportController.downloadPdf);

export default router;
