#!/usr/bin/env bash
# fail this script if a command fails
set -e

# Set up git user name and tag this commit
git config --local user.name "Andreas Keefer (Travis CI)"
git config --local user.email "xtermi2@users.noreply.github.com"
git tag $TRAVIS_TAG -m "Release ${PROJECT_VERSION}"
#debug
set | grep TRAVIS && set | grep SNAPSHOT && set | grep PROJECT