server {
    server_name iamazim.com www.iamazim.com;

    location / {
        proxy_pass http://127.0.0.1:3010;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/iamazim.com/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/iamazim.com/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot


}

server {
    server_name api.iamazim.com;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/iamazim.com/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/iamazim.com/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}

server {
    server_name voice.iamazim.com;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/iamazim.com/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/iamazim.com/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}
server {
    if ($host = www.iamazim.com) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    if ($host = iamazim.com) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    listen 80;
    server_name iamazim.com www.iamazim.com;
    return 404; # managed by Certbot




}

server {
    if ($host = api.iamazim.com) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    listen 80;
    server_name api.iamazim.com;
    return 404; # managed by Certbot


}

server {
    if ($host = voice.iamazim.com) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    listen 80;
    server_name voice.iamazim.com;
    return 404; # managed by Certbot


}