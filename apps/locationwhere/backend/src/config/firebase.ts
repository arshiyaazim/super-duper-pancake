import * as admin from 'firebase-admin';
import fs from 'fs';
import logger from '../utils/logger';

const getFirebaseCredential = (): admin.ServiceAccount | null => {
  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (serviceAccountPath) {
    const raw = fs.readFileSync(serviceAccountPath, 'utf8');
    return JSON.parse(raw) as admin.ServiceAccount;
  }

  const firebaseConfig = {
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
  };

  if (firebaseConfig.projectId && firebaseConfig.clientEmail && firebaseConfig.privateKey) {
    return firebaseConfig as admin.ServiceAccount;
  }

  return null;
};

try {
  if (!admin.apps.length) {
    const credential = getFirebaseCredential();
    if (credential) {
      admin.initializeApp({
        credential: admin.credential.cert(credential),
      });
      logger.info('Firebase Admin SDK initialized');
    } else {
      logger.warn('Firebase Admin SDK not initialized: missing service account configuration');
    }
  }
} catch (error) {
  logger.error('Firebase Admin SDK initialization failed', error);
}

export const isFirebaseAdminInitialized = () => admin.apps.length > 0;

export default admin;
