#!/usr/bin/env bash

# don't fail this script if a single command fails
set +e
echo $GPG_SECRET_KEYS | base64 --decode | $GPG_EXECUTABLE --import
echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust
set -e

echo imported!