#!/bin/bash
# Called by SSM as root; arguments are the immutable image reference and AWS region.
set -euo pipefail
cd /opt/hmc
test -s .env
image=$1
region=$2
registry=${image%%/*}
aws ecr get-login-password --region "$region" | docker login --username AWS --password-stdin "$registry"
export APP_IMAGE="$image"
docker compose pull
docker compose up -d --wait --wait-timeout 180
app_address=$(docker compose port app 8080 | head -n 1)
app_port=${app_address##*:}
for attempt in $(seq 1 60); do
  if curl --fail --silent "http://127.0.0.1:$app_port/actuator/health" > /dev/null; then
    printf 'APP_IMAGE=%s\n' "$image" > .app-image
    exit 0
  fi
  sleep 3
done
docker compose logs --tail 100 app
exit 1
