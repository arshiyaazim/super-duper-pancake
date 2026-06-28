# Employee Monitoring Application Backend

## Technology Stack
- **Node.js v20** with **Express.js**
- **PostgreSQL 16** (Database)
- **Redis 7** (Live Location Cache)
- **Prisma** (ORM)
- **AWS S3** (Encrypted Call Recordings)
- **Firebase Admin SDK** (Push Notifications)
- **Zod** (Validation)

## Getting Started

1. **Install dependencies:**
   ```bash
   cd backend
   npm install
   ```

2. **Setup environment variables:**
   Copy `.env.example` to `.env` and fill in your credentials.

3. **Database Migration:**
   ```bash
   npx prisma migrate dev
   ```

4. **Run development server:**
   ```bash
   npm run dev
   ```

## API Modules

### Auth
- `POST /api/v1/auth/login` - Admin Login
- `POST /api/v1/auth/mobile/login` - Employee App Login

### Employee
- `GET /api/v1/employees` - List employees (Admin only)
- `POST /api/v1/employees/:id/consent` - Submit legal consent

### Location
- `POST /api/v1/location/update` - Send GPS data (every 30s)
- `GET /api/v1/location/live` - Real-time tracking dashboard

### SIM & Security
- `POST /api/v1/sim/change-alert` - Automated SIM change detection

### Calls
- `POST /api/v1/calls/log` - Sync call history
- `POST /api/v1/calls/upload-recording` - Upload encrypted audio to S3

## Security Features
- **JWT Authentication:** 15m Access Token + 7d Refresh Token.
- **RBAC:** Role-based access control for Super Admin, HR, and Branch Managers.
- **Encryption:** AES-256-CBC for sensitive data and call recordings.
- **Rate Limiting:** Protection against brute force and API abuse.
