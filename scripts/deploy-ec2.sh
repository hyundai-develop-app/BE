#!/bin/bash
# Called as root (by SSM or autodeploy-ec2.sh); arguments are the immutable image reference
# and, for ECR images only, the AWS region.
set -euo pipefail
cd /opt/hmc
test -s .env
image=$1
region=${2:-}
export APP_IMAGE="$image"
registry=${image%%/*}
on_fail() {
  status=$?
  trap - ERR
  docker compose ps || true
  docker compose logs --tail 200 app postgres redis || true
  exit "$status"
}
trap on_fail ERR
# Public GHCR images need no login; only ECR does.
case "$registry" in
  *.dkr.ecr.*.amazonaws.com)
    aws ecr get-login-password --region "$region" | docker login --username AWS --password-stdin "$registry"
    ;;
esac
docker compose pull app
docker compose up -d --wait --wait-timeout 180
app_address=$(docker compose port app 8080 | head -n 1)
app_port=${app_address##*:}
for attempt in $(seq 1 60); do
  if curl --fail --silent "http://127.0.0.1:$app_port/actuator/health" > /dev/null; then
    printf 'APP_IMAGE=%s\n' "$image" > .app-image
    docker image prune -af --filter 'label=org.opencontainers.image.title=hmc-be' --filter 'until=72h' || true
    exit 0
  fi
  sleep 3
done
false # Trigger ERR diagnostics after health timeout.
