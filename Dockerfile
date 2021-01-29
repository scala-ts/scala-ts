FROM cchantep/sbt:jdk8u265-b01-alpine-slim-akka-node14-yarn12

ARG BACKEND_URL
ADD . /opt/webapp

RUN cd /opt/webapp && \
echo "Backend URL: $BACKEND_URL" && \
sbt 'common/compile' && \
cd frontend/ && \
yarn build && \
cd .. && \
cp -R frontend/dist http-api/src/main/resources/webroot && \
sbt http-api/universal:stage && \
adduser -D web && \
chown -R web /opt/webapp

USER web
EXPOSE 9000
CMD /opt/webapp/http-api/target/universal/stage/bin/scala-ts-demo-api
