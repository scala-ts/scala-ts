# Scala-TS demo

Scala-TS demonstration with an [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html) API sharing Scala type with a TypeScript [Svelte](https://svelte.dev/) frontend.

## Build

Using [SBT](https://www.scala-sbt.org/):

1. Run `sbt common/compile` to compile the Scala data model, and generate the corresponding TypeScript (to `common/target/scala-ts/src_managed/`).
2. Run `sbt http-api/compile` to compile the REST API.

Since *#1* is ok, the TypeScript/Svelte frontend can be built (using the generated TypeScript).

```bash
cd frontend
yarn build
```

**Run locally:**

Scala REST API:

    sbt http-api/run

Frontend:

```bash
cd frontend
yarn dev
```

## Deploy

On Heroku *(see [heroku.yml](./heroku.yml) & [Dockerfile](./Dockerfile))*

    git push heroku demo/akka-http-svlete:master
