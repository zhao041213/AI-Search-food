#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/opt/ai-search-food"
STAGED_JAR="/tmp/ai-search-food-0.0.1-SNAPSHOT.jar"
SERVICE_FILE="/tmp/ai-search-food.service"
NGINX_FILE="/tmp/ai-search-food.nginx.conf"

install -m 755 -d "${APP_DIR}"
install -m 644 "${STAGED_JAR}" "${APP_DIR}/app.jar"
chown -R ai-search-food:ai-search-food "${APP_DIR}"

install -m 644 "${SERVICE_FILE}" /etc/systemd/system/ai-search-food.service
install -m 644 "${NGINX_FILE}" /etc/nginx/conf.d/ai-search-food.conf

systemctl daemon-reload
systemctl enable ai-search-food.service
systemctl restart ai-search-food.service

nginx -t
systemctl reload nginx

systemctl --no-pager --full status ai-search-food.service
