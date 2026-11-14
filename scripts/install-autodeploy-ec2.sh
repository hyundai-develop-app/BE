#!/bin/bash
# One-time setup on the EC2 host (run as root, e.g. through SSM from CloudShell).
# Installs autodeploy-ec2.sh and a systemd timer that runs it every minute.
# Optional argument: git ref to fetch the script from (default: develop).
set -euo pipefail
ref=${1:-develop}
source_repo=${AUTODEPLOY_SOURCE_REPO:-hyundai-develop-app/BE}
cd /opt/hmc
test -s .env

curl -fsSL "https://raw.githubusercontent.com/${source_repo}/${ref}/scripts/autodeploy-ec2.sh" -o autodeploy.sh.new
install -m 0755 autodeploy.sh.new autodeploy.sh
rm -f autodeploy.sh.new

cat > /etc/systemd/system/hmc-autodeploy.service << 'UNIT'
[Unit]
Description=HMC-BE deploy of new GHCR image
After=docker.service network-online.target
Requires=docker.service

[Service]
Type=oneshot
ExecStart=/bin/bash /opt/hmc/autodeploy.sh
TimeoutStartSec=900
UNIT

cat > /etc/systemd/system/hmc-autodeploy.timer << 'UNIT'
[Unit]
Description=Check GHCR for a new HMC-BE image every minute

[Timer]
OnBootSec=2min
OnUnitActiveSec=1min

[Install]
WantedBy=timers.target
UNIT

systemctl daemon-reload
systemctl enable --now hmc-autodeploy.timer
systemctl list-timers hmc-autodeploy.timer --no-pager
