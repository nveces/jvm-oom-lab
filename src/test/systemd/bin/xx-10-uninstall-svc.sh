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

if (( EUID != 0 )); then
  echo "Please, run this command with sudo" 1>&2
  exit 1
else
  echo "I'm sudo"
fi

SVC_PREFIX_NAME="jvm-oom@"
SVC_NAME="${SVC_PREFIX_NAME}.service"
SVC_PATH="/etc/systemd/system/${SVC_NAME}"

echo "Uninstalling the ${SVC_NAME} service..."
systemctl stop "${SVC_PREFIX_NAME}*" 2>/dev/null
systemctl disable ${SVC_NAME} 2>/dev/null

# 3. Eliminar el archivo
if [ -f "${SVC_NAME}" ]; then
    rm "${SVC_NAME}"
    echo "✅ The ${SVC_NAME} service Deleted"
else
  echo "The ${SVC_NAME} service doesn't exist"
fi

# 4. Recargar systemd
systemctl daemon-reload

# Al final de xx-10-uninstall-svc.sh
if ! systemctl list-unit-files | grep -q "$SVC_NAME"; then
    echo "✅ Cleanup verified: Service unit has been removed."
fi

echo "daemon-reload executed. Bye!"

exit 0

# EOF