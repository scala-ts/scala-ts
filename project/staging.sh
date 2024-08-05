#! /bin/sh

export PUBLISH_REPO_NAME="Sonatype Nexus Repository Manager"
export PUBLISH_REPO_ID="oss.sonatype.org"
export PUBLISH_REPO_URL="https://oss.sonatype.org/service/local/staging/deploy/maven2"

if [ -z "$PUBLISH_USER" ]; then
  echo "User: "
  read PUBLISH_USER
fi

if [ -z "$PUBLISH_PASS" ]; then
  echo "Password: "
  read PUBLISH_PASS
fi

sbt
