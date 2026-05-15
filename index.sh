#!/bin/bash

# ==========================================
# Let's Encrypt SSL Auto Renew Script
# ==========================================

LOG_FILE="/var/log/ssl-renew.log"

echo "==========================================" >> $LOG_FILE
echo "[START] $(date)" >> $LOG_FILE

# SSL 갱신 시도
certbot renew --quiet

# 결과 확인
if [ $? -eq 0 ]; then
    echo "[SUCCESS] SSL renewal completed" >> $LOG_FILE

    # nginx reload
    systemctl reload nginx

    echo "[SUCCESS] nginx reload completed" >> $LOG_FILE
else
    echo "[ERROR] SSL renewal failed" >> $LOG_FILE
fi

echo "[END] $(date)" >> $LOG_FILE
echo "==========================================" >> $LOG_FILE
echo "" >> $LOG_FILE