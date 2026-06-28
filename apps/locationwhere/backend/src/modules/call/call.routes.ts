import { Router } from 'express';
import multer from 'multer';
import * as callController from './call.controller';
import { authAdmin, authEmployee } from '../../middleware/auth.middleware';

const router = Router();
const upload = multer({ storage: multer.memoryStorage() });

router.post('/log', authEmployee, callController.logCall);
router.post('/upload-recording', authEmployee, upload.single('recording'), callController.uploadRecording);
router.get('/', authAdmin, callController.getCalls);
router.get('/:id/recording', authAdmin, callController.getRecording);

export default router;
