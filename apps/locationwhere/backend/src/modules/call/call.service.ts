import prisma from '../../config/database';
import s3Client from '../../config/aws';
import { PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { verifyChecksum } from '../../utils/encryption';

const isS3Configured = () =>
  Boolean(
    process.env.AWS_ACCESS_KEY_ID?.trim() &&
    process.env.AWS_SECRET_ACCESS_KEY?.trim() &&
    process.env.AWS_REGION?.trim() &&
    process.env.AWS_S3_BUCKET?.trim()
  );

const assertS3Configured = () => {
  if (!isS3Configured()) {
    throw new Error('Call recording storage is not configured');
  }
};

export const logCall = async (employeeId: string, callData: any) => {
  return prisma.callLog.create({
    data: {
      employeeId,
      ...callData,
      startedAt: new Date(callData.startedAt),
      endedAt: new Date(callData.endedAt)
    }
  });
};

export const uploadRecording = async (callLogId: string, file: Express.Multer.File, checksum: string) => {
  assertS3Configured();

  if (!verifyChecksum(file.buffer, checksum)) {
    throw new Error('Recording checksum verification failed');
  }

  const key = `recordings/${callLogId}_${Date.now()}.enc`;

  await s3Client.send(new PutObjectCommand({
    Bucket: process.env.AWS_S3_BUCKET,
    Key: key,
    Body: file.buffer,
    ContentType: file.mimetype
  }));

  return prisma.callLog.update({
    where: { id: callLogId },
    data: {
      hasRecording: true,
      recordingPath: key,
      recordingEncrypted: true
    }
  });
};

export const getRecordingUrl = async (id: string) => {
  assertS3Configured();

  const call = await prisma.callLog.findUnique({ where: { id } });
  if (!call || !call.recordingPath) throw new Error('Recording not found');

  const command = new GetObjectCommand({
    Bucket: process.env.AWS_S3_BUCKET,
    Key: call.recordingPath
  });

  return getSignedUrl(s3Client, command, { expiresIn: 900 }); // 15 min
};
