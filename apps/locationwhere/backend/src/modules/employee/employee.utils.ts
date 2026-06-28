import crypto from 'crypto';
import { PrismaClient } from '@prisma/client';

const EMPLOYEE_CODE_PATTERN = /^EMP(\d+)$/i;
const BANGLADESH_MOBILE_PATTERN = /(?:\+?88)?01\d(?:[\s-]?\d){8}/;

export const normalizePhoneNumber = (value: string) => {
  const digits = value.replace(/\D/g, '');

  if (digits.startsWith('880') && digits.length === 13) {
    return `0${digits.slice(3)}`;
  }

  if (digits.startsWith('01') && digits.length === 11) {
    return digits;
  }

  throw new Error('Phone number must be a valid Bangladesh mobile number');
};

export const normalizeSmsNumber = (value: string) => {
  const localNumber = normalizePhoneNumber(value);
  return `880${localNumber.slice(1)}`;
};

export const maskPhoneNumber = (value: string) => {
  const localNumber = normalizePhoneNumber(value);
  return `${localNumber.slice(0, 3)}******${localNumber.slice(-2)}`;
};

export const buildPlaceholderEmail = (employeeCode: string) =>
  `${employeeCode.toLowerCase()}@onboarding.locationwhere.local`;

export const generatePassword = () => {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
  const bytes = crypto.randomBytes(10);

  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join('');
};

const getHighestEmployeeSequence = async (prisma: PrismaClient) => {
  const employees = await prisma.employee.findMany({
    select: { employeeCode: true }
  });

  return employees.reduce((highest, employee) => {
    const match = employee.employeeCode.match(EMPLOYEE_CODE_PATTERN);
    if (!match) return highest;
    return Math.max(highest, Number(match[1]));
  }, 0);
};

export const generateNextEmployeeCode = async (prisma: PrismaClient) => {
  const nextSequence = (await getHighestEmployeeSequence(prisma)) + 1;
  return `EMP${String(nextSequence).padStart(3, '0')}`;
};

export const parseOnboardingSms = (rawMessage: string) => {
  const message = rawMessage.trim();
  const idMatch = message.match(/^I\s*D\s*:?\s*(.+)$/i);
  if (!idMatch) {
    throw new Error('SMS body must include ID and a mobile number');
  }

  const content = idMatch[1].trim();
  const phoneMatch = content.match(BANGLADESH_MOBILE_PATTERN);
  if (!phoneMatch) {
    throw new Error('SMS body must include ID and a mobile number');
  }

  const phone = normalizePhoneNumber(phoneMatch[0]);
  const fullName = content
    .replace(phoneMatch[0], ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return {
    phone,
    fullName: fullName || phone
  };
};
