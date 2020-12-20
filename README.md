# ScalaTS

*ScalaTS* generate TypeScript from Scala.

Help to integrate REST-ful Scala backend and TypeScript frontend.
Having defined Scala types returned by your endpoints you can easily generate TypeScript definitions for consuming these endpoints.

## Usage

*ScalaTS* can be used either standalone or as a SBT plugin.

See:

- [Usage details](docs/index.md#usage)
- Blog post: [Scala-ts: Scala to TypeScript code generator](http://codewithstyle.info/scala-ts-scala-typescript-code-generator/) at Code with Style.

## Build manually

The core library and compiler plugin can be built using [SBT](https://www.scala-sbt.org).

    sbt +core/publishLocal

The SBT plugin can also be built:

    sbt '^ sbt-plugin/publishLocal'

*Running tests:* [![Travis build status](https://travis-ci.org/scala-ts/scala-ts.svg?branch=master)](https://travis-ci.org/scala-ts/scala-ts):

The tests for the core library and compiler plugin can be executed.

    sbt +core/test

The [scripted tests](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html) for the SBT plugins can also be executed.

    sbt ';^ sbt-plugin/testOnly ;^ sbt-plugin/scripted'

Considering a single scripted tests (e.g. `simple`), it can be executed interactively for development purpose.

```bash
export SCRIPTED_TEST="simple"
export PLUGIN_VERSION="0.4.1-SNAPSHOT"
export SBT_VERSION="1.4.4"

cd "sbt-plugin/src/sbt-test/scala-ts-sbt/${SCRIPTED_TEST}"
sbt "-J-Dscala-ts.version=${PLUGIN_VERSION}" "-J-Dsbt.version=${SBT_VERSION}"
```

## Credits

Many thanks to all the [contributors](https://github.com/scala-ts/scala-ts/graphs/contributors).

* [@nicolasdalsass](https://github.com/nicolasdalsass) who create a [forked project](https://github.com/Elium/scala-ts/tree/master). Some of his ideas have been integrated back into `scala-ts`.
* [@returntocorp](https://github.com/returntocorp) for the SBT 1.0, `Either`, `Map` support.
