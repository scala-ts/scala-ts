# Scala-TS demo

Scala-TS demonstration with an [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html) API sharing Scala type with a TypeScript [Svelte](https://svelte.dev/) frontend.

## Build

Using [SBT](https://www.scala-sbt.org/):

    sbt compile

**Run locally:**

    sbt run

## Deploy

On Heroku:

    git push heroku demo/akka-http-svlete:master
