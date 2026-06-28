# Nginx Configuration

## Setup on VPS

1. Copy config to nginx sites:
   sudo cp locationwhere.conf.example /etc/nginx/sites-available/locationwhere
   sudo ln -sf /etc/nginx/sites-available/locationwhere /etc/nginx/sites-enabled/

2. Create downloads directory:
   sudo mkdir -p /var/www/locationwhere.iamazim.com/downloads
   sudo chown www-data:www-data /var/www/locationwhere.iamazim.com/downloads

3. Upload APK:
   scp app-debug.apk user@vps:/var/www/locationwhere.iamazim.com/downloads/

4. Test and reload nginx:
   sudo nginx -t
   sudo systemctl reload nginx
