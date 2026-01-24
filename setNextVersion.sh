#!/usr/bin/env bash
# This script sets the project version in a Maven project to a specified version and pushes the change.
# params:
#   $1 - The new version to set in the project
#   $2 - The git branch to push the changes to, this should be the currently checked out branch
# REQUIRED ENVIRONMENT VARIABLES:
#   MVN_CMD - The Maven command to use (e.g., ./mvnw or mvn)

# fail this script if a command fails
set -e

# set first param to variable NEW_VERSION or fail if the param is not set
if [ -z "$1" ]; then
    echo "NEW_VERSION parameter is required"
    exit 1
fi
NEW_VERSION=$1
if [ -z "$2" ]; then
    echo "GIT_BRANCH parameter is required"
    exit 1
fi
GIT_BRANCH=$2

# ensure MVN_CMD is set before using it
if [ -z "$MVN_CMD" ]; then
    echo "MVN_CMD environment variable is required (e.g., ./mvnw or mvn)"
    exit 1
fi

# Configure git for any operations
# this is already done by github action 'crazy-max/ghaction-import-gpg'
# defaults to the name associated with the GPG key
#git config --global user.name "GitHub Actions"
# defaults to the email address associated with the GPG key
#git config --global user.email "actions@github.com"

CURRENT_VERSION=$($MVN_CMD help:evaluate -Dexpression=project.version -q -DforceStdout)

echo "Current version: '$CURRENT_VERSION'"
echo "Target version: '${NEW_VERSION}'"
echo "Working on branch: '${GIT_BRANCH}'"

# Debug: Show recent commits
echo "Recent commits:"
git log --oneline -5

# Check if we already have the commit
COMMIT_MESSAGE="Set version to '${NEW_VERSION}'"
if GIT_PAGER=cat git log --oneline -10 | grep -q "${COMMIT_MESSAGE}"; then
  echo "commit already exists, skipping version setting"
elif [ "$CURRENT_VERSION" != "${NEW_VERSION}" ]; then
  ./setVersion.sh "${NEW_VERSION}"
  git add pom.xml "**/pom.xml"
  # debug: show the status of the git repo before commit an push
  git status
  git commit -m "${COMMIT_MESSAGE}"
  git push origin "${GIT_BRANCH}"
else
  echo "Version is already set to '${NEW_VERSION}'"
fi