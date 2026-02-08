#!/usr/bin/env bash
# This script sets the project version in a Maven project to a specified version.
# params:
#   $1 - The new version to set in the project
# REQUIRED ENVIRONMENT VARIABLES:
#   MVN_CMD - The Maven command to use (e.g., ./mvnw or mvn)

# fail this script if a command fails
set -e

# set first param to variable VERSION or fail if the param is not set
if [ -z "$1" ]; then
    echo "VERSION parameter is required"
    exit 1
fi
VERSION=$1

# ensure MVN_CMD is set before using it
if [ -z "$MVN_CMD" ]; then
    echo "MVN_CMD environment variable is required (e.g., ./mvnw or mvn)"
    exit 1
fi

echo "Setting version to '${VERSION}'"
$MVN_CMD versions:set -DgenerateBackupPoms=false -DnewVersion="${VERSION}"

#loop over all directories starting with "tests-"
for dir in tests-*/ ; do
  if [ -d "$dir" ]; then
    # now loop over all directories inside this directory
    for subdir in "$dir"*/ ; do
      if [ -d "$subdir" ]; then
        # check if a "pom.xml" file exists in this subdirectory
        # and the subdir is not named `migration-scripts` (this is part of the main project and is already handled above)
        subdirname=$(basename "$subdir")
        if [ -f "$subdir/pom.xml" ] && [ "$subdirname" != "migration-scripts" ]; then
          echo "Setting version to '${VERSION}' in directory '$subdir'"
          $MVN_CMD -f "$subdir/pom.xml" versions:set -DgenerateBackupPoms=false -DnewVersion="${VERSION}"
        fi
      fi
    done
  fi
done