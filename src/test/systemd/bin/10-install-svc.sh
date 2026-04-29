#!/bin/bash
#
# ============================================================
#
#
# Created-------:
# ============================================================
# Description--:
# ============================================================
#
# ============================================================
# Pre Steps---:
# chmod 774 *.sh
# ============================================================
#
#
#
# EOH

#set -euo pipefail
set -uo pipefail

# Step 1: Set current DIR and default variables:
SCRIPT_DIR=$(dirname $(realpath $0))
# Navigate up 4 levels to reach the project root directory
# (bin <- systemd <- test <- src <- ROOT)
PROJECT_ROOT=$(realpath "$(dirname "$0")/../../../../")
SVC_NAME="jvm-oom@.service"
# Path where Maven will place the filtered service file
SVC_FILTERED="${PROJECT_ROOT}/target/systemd-config/${SVC_NAME}"
SYSTEMD_DEST="/etc/systemd/system/"
echo "--- Preparing Systemd Laboratory ---"
echo "Project Root ---: ${PROJECT_ROOT}"
echo "Script Folder --: ${SCRIPT_DIR}"
echo "Service filtered: ${SVC_FILTERED}"

if (( EUID != 0 )); then
  echo "Please, run this command with sudo" 1>&2
  exit 1
else
  echo "I'm sudo"
fi

if [ ! -f "$SVC_FILTERED" ]; then
  echo "Error: Filtered service file not found at $SVC_FILTERED"
  echo "Make sure the 'systemd' profile is correctly configured in your pom.xml"
  exit 1
fi

echo "Installing the service..."
cp "${SVC_FILTERED}" ${SYSTEMD_DEST}
chmod 644 "${SYSTEMD_DEST}${SVC_NAME}"
echo "Reloading systemd daemon..."
systemctl daemon-reload

echo "✅ Installation successful."
echo "You can now start a scenario, for example: systemctl start jvm-oom@heap"

echo "Verifying unit file load status..."
if systemctl list-unit-files | grep -q "$SVC_NAME"; then
    echo "✅ Service unit $SVC_NAME is successfully loaded."
else
    echo "❌ Error: Service unit $SVC_NAME was not found by systemd."
    exit 1
fi

exit 0

# EOF