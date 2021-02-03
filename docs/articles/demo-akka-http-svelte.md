---
layout: default
---

TODO: Add in index
**More:**

- [How to share data model between Akka HTTP API and TypeScript Svelte frontend](./articles/demo-akka-http-svelte.html)


# Akka HTTP/Svelte demonstration

This is a *Scala-TS* demonstration with a REST API managing user information, and a TypeScript [SPA](https://en.wikipedia.org/wiki/Single-page_application) as frontend for this API.

The Scala API is developed using [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html), while the TypeScript frontend is based on [Svelte](https://svelte.dev/).

This sample project will demonstrate how to share the [data model](#model) between the Scala REST API and the TypeScript frontend in such situation.

> See [sources on GitHub](https://github.com/scala-ts/scala-ts/tree/demo/akka-http-svlete)

> Try it [on Heroku](https://scala-ts-demo.herokuapp.com/)

## Use cases

The application demonstrates several user management features.

![Demonstration use cases](../assets/demo-akka-http-svelte/usecases.svg)

### Signup

The user can create a new account.

First, the user signs up using the Svlete form.

![SignUp form](../assets/demo-akka-http-svelte/signup1.png)

Then the frontend `POST`s as JSON the TypeScript `Account` (see in model; TODO: Link).

(TODO: Snippet & GitHub Link)

The REST API receives the JSON data and decodes it as Scala `Account` (see in model; TODO: Link).

(TODO: Scala Snippet & GitHub link)

> *Note:* [Play JSON](https://github.com/playframework/play-json#play-json) is used to read and write the Scala types from/to JSON requests/responses.

If it's Ok, the Scala `UserName` (see in model; TODO: Link) is sent back as JSON response.

(TODO: Scala Snippet & GitHub link)

Finally, frontend handles the response as TypeScript `UserName` (see in model; TODO: Link), and the Sign Up confirmation is displayed (or error is some).

> *Note:* In this sample frontend, it's possible to 'quite' safely [assert the JSON response as the expected type](https://www.typescriptlang.org/docs/handbook/basic-types.html#type-assertions). In many case, validating the response must be done (e.g. [io-ts](https://gcanti.github.io/io-ts/), [idonttrustlikethat](https://scala-ts.github.io/scala-ts/#idonttrustlikethat), ...).

(TODO: Scala Snippet & GitHub link)

TODO: Screenshot

### Login

The user can connect using his credentials, and then see his information on the profile screen.

First, the user type his name and password.

TODO: Screenshot

Then the frontend `POST`s as JSON the TypeScript `Credentials` (see in model; TODO: Link).

(TODO: Scala Snippet & GitHub link)

The REST API receives the JSON data and decodes it as Scala `Credentials` (see in model; TODO: Link).

(TODO: Scala Snippet & GitHub link)

If it's Ok, a Scala `UserToken` is sent back as JSON response.

(TODO: Scala Snippet & GitHub link)

Then the frontend redirects to the profile screen, which `GET`s its information (according the token passed as [HTTP authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)).

(TODO: Scala Snippet & GitHub link)

The REST API handles the user token, and finds the corresponding Scala `Account` to send it back as JSON.

(TODO: Scala Snippet & GitHub link)

Finally, the frontend receives the response as TypeScript `Account` (see in model; TODO: Link), and it's displayed on the profile screen.

(TODO: Scala Snippet & GitHub link)

TODO: Screenshot

## Model

The demonstration models the user accounts and relared information (username, credentials and token).

![Demonstration use cases](../assets/demo-akka-http-svelte/usecases.svg)

TODO: Links in the table (to GitHub)

| Scala                  | TypeScript            |
| ---------------------- | --------------------- |
| Account case class     | Account interface     |
| UserName value class   | string                |
| Credentials case class | Credentials interface |
| UserToken value class  | string                |

## Build

Using [SBT](https://www.scala-sbt.org/):

1. Run `sbt common/compile` to compile the Scala data model, and generate the corresponding TypeScript (to `common/target/scala-ts/src_managed/`).
2. Run `sbt http-api/compile` to compile the REST API.

Since *#1* is ok, the TypeScript/Svelte frontend can be built (using the generated TypeScript).

```bash
cd frontend
yarn build
```

## Deploy

heroku.yml & Dockerfile (copy frontend to src/main/resources/webroot so serve as static resources)

