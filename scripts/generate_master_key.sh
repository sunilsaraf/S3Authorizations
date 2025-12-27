#!/usr/bin/env bash
# Generate a base64 32-byte key for APP_MASTER_KEY
head -c 32 /dev/urandom | base64