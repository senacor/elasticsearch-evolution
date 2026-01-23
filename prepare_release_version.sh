#!/usr/bin/env bash
# fail this script if a command fails
set -e

#set project version from maven version
./mvnw help:evaluate -N -Dexpression=project.version|grep -v '\['
export PROJECT_VERSION=$(./mvnw help:evaluate -N -Dexpression=project.version|grep -v '\[')
# check if this is a snapshot version
export IS_SNAPSHOT=$(if [[ $PROJECT_VERSION == *"-SNAPSHOT" ]]; then echo true; else echo false;fi)

export TRAVIS_TAG=${PROJECT_VERSION}-$(date +'%Y-%m-%dT%H-%M-%S')-$(git log --format=%h -1)

# export the current git commit sha
export RELEASE_GIT_COMMIT_SHA=$(git rev-parse HEAD)
# export the current git commit message
export RELEASE_GIT_COMMIT_MESSAGE=$(git log -1 --pretty=%B)
echo "Release will be made from commit ${RELEASE_GIT_COMMIT_SHA}: ${RELEASE_GIT_COMMIT_MESSAGE}"

if [ ! -z "$GITHUB_ENV" ]; then
    echo "PROJECT_VERSION=${PROJECT_VERSION}" >> $GITHUB_ENV
    echo "TRAVIS_TAG=${TRAVIS_TAG}" >> $GITHUB_ENV
    echo "IS_SNAPSHOT=${IS_SNAPSHOT}" >> $GITHUB_ENV
    echo "RELEASE_GIT_COMMIT_SHA=${RELEASE_GIT_COMMIT_SHA}" >> $GITHUB_ENV

    #debug
    echo "Contents of GITHUB_ENV:"
    cat $GITHUB_ENV
fi


#debug
set | grep TRAVIS && set | grep SNAPSHOT && set | grep PROJECT
#if [ "$IS_SNAPSHOT" == "true" ]; then
#    echo "can't release a snapshot version: $PROJECT_VERSION"
#    exit 1;
#fi