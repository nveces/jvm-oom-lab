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

# Show the restarts:
#journalctl -u "jvm-oom@*" -f -o short-precise --since "10 min ago" --no-hostname -o cat | grep -E "Starting|Started|Terminating|Scheduled restart|Stopped"

# Show the journal for
journalctl -u "jvm-oom@*" -f -o short-precise --since "10 min ago" --no-hostname


exit 0

# EOF