#! /bin/sh

set -e

if [ -z "$PUBLISH_USER" ]; then
  echo "User: "
  read PUBLISH_USER
fi


if [ -z "$PUBLISH_PASS" ]; then
  echo "Password: "
  read PUBLISH_PASS
  echo
fi

NAMESPACE="io.github.scala-ts"

curl -D - -X POST -u "${PUBLISH_USER}:${PUBLISH_PASS}" \
  "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$NAMESPACE"


