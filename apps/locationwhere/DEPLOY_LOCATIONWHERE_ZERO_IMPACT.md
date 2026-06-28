# LocationWhere Zero-Impact Deploy Plan

This deploy plan keeps `fazle-core` untouched:

- no container rebuild
- no system restart
- no shared database schema changes inside `fazle-core`
- isolated backend folder, PM2 app, port, database, and Redis DB index

## Runtime isolation

- Frontend domain: `https://locationwhere.iamazim.com`
- Backend port: `8310`
- PM2 app name: `locationwhere-backend`
- Backend folder: `/home/azim/locationwhere-backend`
- PostgreSQL host: `172.20.0.3`
- Redis host: `172.20.0.5`
- Redis DB index: `15`

## Required isolated resources

Preferred isolation:

- database: `locationwhere`
- user: `locationwhere_user`
- password: unique password not reused by other apps

Temporary fallback accepted for bring-up:

- database: `postgres`
- schema: `locationwhere`
- shared superuser connection only until dedicated DB/user is created

Reuse the existing Redis service with a separate DB index:

- `redis://:PASSWORD@172.20.0.5:6379/15`

## Backend env

Use `backend/.env.locationwhere.example` as the template.

Required values before go-live:

- `DATABASE_URL`
- `REDIS_URL`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`

Optional for first boot:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_S3_BUCKET`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`
- `SMS_API_TOKEN`
- `SMS_SID`
- `SMS_DOMAIN`

## Deploy sequence

1. Upload backend source to `/home/azim/locationwhere-backend`
2. Create `/home/azim/locationwhere-backend/.env`
3. Install dependencies in the isolated folder
4. Run Prisma generate
5. Build TypeScript
6. Start PM2 with `ecosystem.locationwhere.config.cjs`
7. Add nginx `/api/` proxy for `locationwhere.iamazim.com` -> `127.0.0.1:8310`
8. Reload nginx
9. Verify:
   - `https://locationwhere.iamazim.com`
   - `https://locationwhere.iamazim.com/api/v1/health` if proxied
   - Android app login

## Known contract fixes already applied

- Android mobile login now expects password-based auth
- Backend refresh route exists for mobile token renewal
- Dashboard remote commands use `/device/commands`
- Redis is optional and live-location API falls back to PostgreSQL
