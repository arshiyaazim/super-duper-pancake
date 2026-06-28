#!/bin/bash
# deploy.sh — Deploy iamazim.com static website
# Run once from this directory: bash deploy.sh
# Requires sudo.

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="/var/www/iamazim.com"
NGINX_CONF="/etc/nginx/sites-enabled/iamazim.com.conf"

echo "==> Creating web directory..."
sudo mkdir -p "$WEB_DIR/legal"
sudo chown -R azim:azim "$WEB_DIR"

echo "==> Copying website files..."
cp "$SCRIPT_DIR/index.html"         "$WEB_DIR/index.html"
cp "$SCRIPT_DIR/legal/privacy.html" "$WEB_DIR/legal/privacy.html"
cp "$SCRIPT_DIR/legal/terms.html"   "$WEB_DIR/legal/terms.html"
cp "$SCRIPT_DIR/legal/contact.html" "$WEB_DIR/legal/contact.html"

echo "==> Setting permissions..."
sudo chown -R www-data:www-data "$WEB_DIR"
sudo chmod -R 755 "$WEB_DIR"

echo "==> Installing nginx config..."
sudo cp "$SCRIPT_DIR/nginx-iamazim.com.conf" "$NGINX_CONF"

echo "==> Testing nginx config..."
sudo nginx -t

echo "==> Reloading nginx..."
sudo nginx -s reload

echo ""
echo "✓ Deployed! Visit: https://iamazim.com"
echo "  Legal pages: https://iamazim.com/legal/privacy"
echo "               https://iamazim.com/legal/terms"
echo "               https://iamazim.com/legal/contact"
