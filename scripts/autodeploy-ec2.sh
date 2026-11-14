#!/bin/bash
# Run by the hmc-autodeploy systemd timer as root (see install-autodeploy-ec2.sh).
# Checks the tracked GHCR tag; when its digest differs from the last successful deploy,
# fetches compose/deploy files from the image's source commit and runs deploy.sh.
set -euo pipefail
cd /opt/hmc
test -s .env

tracked=${AUTODEPLOY_IMAGE:-ghcr.io/hyundai-develop-app/hmc-be:develop}
source_repo=${AUTODEPLOY_SOURCE_REPO:-hyundai-develop-app/BE}
repository=${tracked%:*}

exec 9> /opt/hmc/.autodeploy.lock
flock -n 9 || exit 0

docker pull --quiet "$tracked" > /dev/null
image=$(docker image inspect --format '{{range .RepoDigests}}{{println .}}{{end}}' "$tracked" \
  | grep -m 1 "^${repository}@sha256:")

current=$(sed -n 's/^APP_IMAGE=//p' .app-image 2> /dev/null || true)
[ "$image" = "$current" ] && exit 0
# Do not retry a digest that already failed; the next push produces a new digest.
[ "$image" = "$(cat .autodeploy-failed 2> /dev/null || true)" ] && exit 0

revision=$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$tracked")
raw="https://raw.githubusercontent.com/${source_repo}/${revision:-develop}"
echo "[autodeploy] deploying $image (revision ${revision:-unknown})"

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
curl -fsSL "$raw/docker-compose.prod.yml" -o "$tmp/docker-compose.yml"
curl -fsSL "$raw/scripts/deploy-ec2.sh" -o "$tmp/deploy.sh"
curl -fsSL "$raw/scripts/autodeploy-ec2.sh" -o "$tmp/autodeploy.sh"
install -m 0644 "$tmp/docker-compose.yml" docker-compose.yml
install -m 0755 "$tmp/deploy.sh" deploy.sh

if bash deploy.sh "$image"; then
  rm -f .autodeploy-failed
  # Self-update only after a successful deploy so a broken script cannot lock us out.
  install -m 0755 "$tmp/autodeploy.sh" autodeploy.sh
  echo "[autodeploy] success $image"
else
  echo "$image" > .autodeploy-failed
  echo "[autodeploy] FAILED $image" >&2
  exit 1
fi
