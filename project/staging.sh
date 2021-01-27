#! /bin/sh

export PUBLISH_REPO_NAME="Sonatype Nexus Repository Manager"
export PUBLISH_REPO_ID="oss.sonatype.org"
export PUBLISH_USER="cchantep"
export PUBLISH_REPO_URL="https://oss.sonatype.org/service/local/staging/deploy/maven2/"

echo "Password: "
read PASS
export PUBLISH_PASS="$PASS"

sbt
