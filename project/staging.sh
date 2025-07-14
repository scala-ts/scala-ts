#! /bin/sh

set -e

export PUBLISH_REPO_NAME="OSSRH Staging API Service"
export PUBLISH_REPO_ID="ossrh-staging-api.central.sonatype.com"
export PUBLISH_REPO_URL="https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"

if [ -z "$PUBLISH_USER" ]; then
  echo "User: "
  read PUBLISH_USER
fi

if [ -z "$PUBLISH_PASS" ]; then
  echo "Password: "
  read PUBLISH_PASS
  echo
fi

export PUBLISH_USER
export PUBLISH_PASS

exec sbt "$@"
