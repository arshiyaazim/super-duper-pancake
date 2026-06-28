#!/bin/bash
set -e

echo "=== Location Where — VPS Deploy Script ==="
echo ""

# ── Check env vars are set ──
check_env() {
  if [ -z "${!1}" ]; then
    echo "❌ ERROR: $1 is not set in environment"
    exit 1
  fi
  echo "✅ $1 is set"
}

echo "Checking required environment variables..."
check_env "DATABASE_URL"
check_env "JWT_ACCESS_SECRET"
check_env "JWT_REFRESH_SECRET"
check_env "SMS_GATEWAY_SECRET"
check_env "ADMIN_ONBOARDING_PHONE"
check_env "APK_DOWNLOAD_URL"
echo ""

# ── Pull latest code ──
echo "Pulling latest code..."
git pull origin main
echo ""

# ── Install dependencies ──
echo "Installing backend dependencies..."
cd backend
npm ci --production=false
echo ""

# ── Build TypeScript ──
echo "Building backend..."
npm run build
echo ""

# ── Run migrations ──
echo "Running database migrations..."
npx prisma migrate deploy
echo ""

# ── Build admin dashboard ──
echo "Building admin dashboard..."
cd ../admin-dashboard
npm ci
npm run build
echo ""

# ── Copy dashboard dist to web root ──
echo "Deploying admin dashboard..."
sudo cp -r dist/* /var/www/locationwhere.iamazim.com/dist/
echo ""

# ── Restart backend ──
echo "Restarting backend..."
cd ../backend
pm2 restart location-where || pm2 start dist/app.js --name location-where
echo ""

echo "=== Deploy complete ==="
echo ""
pm2 status
