import crypto from 'crypto';

const algorithm = 'aes-256-cbc';
const key = Buffer.from(process.env.ENCRYPTION_KEY || '01234567890123456789012345678901');
const iv = Buffer.from(process.env.ENCRYPTION_IV || '0123456789012345');

export const encryptAES256 = (text: string): string => {
  const cipher = crypto.createCipheriv(algorithm, key, iv);
  let encrypted = cipher.update(text, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  return encrypted;
};

export const decryptAES256 = (encrypted: string): string => {
  const decipher = crypto.createDecipheriv(algorithm, key, iv);
  let decrypted = decipher.update(encrypted, 'hex', 'utf8');
  decrypted += decipher.final('utf8');
  return decrypted;
};

export const generateChecksum = (data: Buffer): string => {
  return crypto.createHash('sha256').update(data).digest('hex');
};

export const verifyChecksum = (data: Buffer, checksum: string): boolean => {
  return generateChecksum(data) === checksum;
};
