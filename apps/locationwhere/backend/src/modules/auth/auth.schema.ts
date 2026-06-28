import { z } from 'zod';

export const loginSchema = z.object({
  body: z.object({
    username: z.string().min(3),
    password: z.string().min(6),
  }),
});

export const mobileLoginSchema = z.object({
  body: z.object({
    employeeCode: z.string(),
    password: z.string(),
    deviceId: z.string(),
    fcmToken: z.string().optional(),
  }),
});

export const refreshSchema = z.object({
  body: z.object({
    refreshToken: z.string(),
  }),
});
