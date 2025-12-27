#!/usr/bin/env bash
# Template script to create an iSCSI target and a file-backed LUN using targetcli.
#
# WARNING: This script must run as root on the iSCSI target host and targetcli must be installed.
# The backing file path should be on a filesystem that is actually backed by the NFS export (or is the NFS mount).
#
# Usage:
#   sudo ./create_iscsi_lun.sh /mnt/nfs/files/mydisk.img 10G iqn.2000-01.org.example:target1 iqn.1993-08.org.debian:01:client-initiator
#
# Parameters:
#  $1 - path to backing file (will be created if missing)
#  $2 - size (e.g. 10G)
#  $3 - target IQN to create (e.g. iqn.2025-12.example:target1)
#  $4 - initiator IQN to allow (e.g. iqn.1993-08.org.debian:01:client-initiator)
set -euo pipefail

BACKING_FILE="${1:-}"
SIZE="${2:-1G}"
TARGET_IQN="${3:-iqn.2025-12.example:target1}"
INITIATOR_IQN="${4:-}"

if [[ -z "$BACKING_FILE" ]]; then
  echo "Usage: $0 /path/to/backing_file.img <size> <target_iqn> [initiator_iqn]"
  exit 2
fi

if [[ ! -d "$(dirname "$BACKING_FILE")" ]]; then
  echo "Creating parent directories for $BACKING_FILE"
  mkdir -p "$(dirname "$BACKING_FILE")"
fi

if [[ ! -f "$BACKING_FILE" ]]; then
  echo "Creating sparse file $BACKING_FILE of size $SIZE"
  fallocate -l "$SIZE" "$BACKING_FILE"
fi

DISK_NAME=$(basename "$BACKING_FILE" | sed 's/[^a-zA-Z0-9]/_/g')

# targetcli commands:
# create a fileio backstore, create an iqn target, create tpg and LUN, add ACL for initiator
sudo targetcli <<EOF
/backstores/fileio create ${DISK_NAME} ${BACKING_FILE}
/iscsi create ${TARGET_IQN}
/iscsi/${TARGET_IQN}/tpg1/luns create /backstores/fileio/${DISK_NAME}
EOF

if [[ -n "$INITIATOR_IQN" ]]; then
  sudo targetcli <<EOF
/iscsi/${TARGET_IQN}/tpg1/acls create ${INITIATOR_IQN}
saveconfig
EOF
else
  sudo targetcli <<EOF
saveconfig
EOF
fi

echo "Created iSCSI target ${TARGET_IQN} with LUN backed by ${BACKING_FILE}"
echo "Run 'targetcli ls' to inspect and 'systemctl restart target' if needed."