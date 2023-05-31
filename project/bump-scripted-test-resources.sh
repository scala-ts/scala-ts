#! /bin/bash

if [ $# -lt 1 ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

VERSION=$1

for TS in `ls -v -1 sbt-*/src/sbt-test/sbt-*/*/src/test/resources/*.ts sbt-*/src/sbt-test/sbt-*/*/*/src/test/resources/*.ts`
do
  echo -n "$TS ..."

  sed -e "s|// Generated by ScalaTS [0-9A-Za-z\\.-]*|// Generated by ScalaTS $VERSION|" < \
    "$TS" > "$TS.tmp"
  mv "$TS.tmp" "$TS"

  echo " OK"
done

for PY in `ls -v -1 */src/sbt-test/sbt-*/*/src/test/resources/*.py`
do
  echo -n "$PY ..."

  sed -e "s|# Generated by ScalaTS [0-9A-Za-z\\.-]*|# Generated by ScalaTS $VERSION|" < \
    "$PY" > "$PY.tmp"
  mv "$PY.tmp" "$PY"

  echo " OK"
done