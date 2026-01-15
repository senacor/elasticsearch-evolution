#!/usr/bin/env bash
# This script pulls an OpenSearch Docker image from Docker Hub (or Amazon ECR if configured)
# and pushes it to Quay.io under the repository quay.io/xtermi2/opensearch.
# This is done because of the rate limits on Docker Hub and Amazon ECR.

# fail this script if a command fails
set -e

# The first argument is the OpenSearch version to publish. It is required.
if [ -z "$1" ]; then
    echo "Usage: $0 <opensearch-version>"
    exit 1
fi
OPENSEARCH_VERSION=$1

#use dockerhub
#SOURCE_REGISTRY=""
#use Amazon ECR
SOURCE_REGISTRY="public.ecr.aws/"

SOURCE_IMAGE="${SOURCE_REGISTRY}opensearchproject/opensearch:${OPENSEARCH_VERSION}"
TARGET_IMAGE="quay.io/xtermi2/opensearch:${OPENSEARCH_VERSION}"

echo "pull ${SOURCE_IMAGE} ..."
docker pull ${SOURCE_IMAGE}

echo
echo "tag pulled image as ${TARGET_IMAGE} ..."
docker tag ${SOURCE_IMAGE} ${TARGET_IMAGE}

echo
echo "push ${TARGET_IMAGE} ..."
docker push ${TARGET_IMAGE}

echo
echo "${TARGET_IMAGE} pushed successfully"