# Scala-TS

*Scala-TS* generate TypeScript from Scala.

Help to integrate REST-ful Scala backend and TypeScript frontend.
Having defined Scala types returned by your endpoints you can easily generate TypeScript definitions for consuming these endpoints.

## Usage

*Scala-TS* can be used either standalone or as a SBT plugin.

See:

- [Usage details](docs/index.md#usage)
- Blog post: [Scala-ts: Scala to TypeScript code generator](http://codewithstyle.info/scala-ts-scala-typescript-code-generator/) at Code with Style.

[![Maven](https://img.shields.io/maven-central/v/io.github.scala-ts/scala-ts-core_2.13.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22scala-ts-core_2.13%22) [![javadoc](https://javadoc.io/badge2/io.github.scala-ts/scala-ts-core_2.13/0.8.0/javadoc.svg)](https://javadoc.io/doc/io.github.scala-ts/scala-ts-core_2.13/0.8.0)

## Build manually

The core library and compiler plugin can be built using [SBT](https://www.scala-sbt.org).

    sbt +core/publishLocal

The SBT plugin can also be built:

    sbt '^ sbt-plugin/publishLocal'

*Running tests:* [![CI](https://github.com/scala-ts/scala-ts/workflows/CI/badge.svg)](https://github.com/scala-ts/scala-ts/actions/workflows/ci.yml)

The tests for the core library and compiler plugin can be executed.

    sbt +core/test

The [scripted tests](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html) for the SBT plugins can also be executed.

    sbt ';^ sbt-plugin/testOnly ;^ sbt-plugin/scripted'

Considering a single scripted tests (e.g. `simple`), it can be executed interactively for development purpose.

```bash
export SCRIPTED_TEST="simple"
export PLUGIN_VERSION="0.5.7"
export SBT_VERSION="1.9.7"

cd "sbt-plugin/src/sbt-test/sbt-scala-ts/${SCRIPTED_TEST}"
sbt "-J-Dscala-ts.version=${PLUGIN_VERSION}" "-J-Dsbt.version=${SBT_VERSION}"
```

Publish on Sonatype:

```bash
./project/staging.sh

project sbt-plugin
^publishSigned
project sbt-plugin-idtlt
^publishSigned
project sbt-plugin-python
^publishSigned
project python
+publishSigned
project idtlt
+publishSigned
project core
+publishSigned
```

## Credits

Many thanks to all the [contributors](https://github.com/scala-ts/scala-ts/graphs/contributors).

* [@nicolasdalsass](https://github.com/nicolasdalsass) who create a [forked project](https://github.com/Elium/scala-ts/tree/master). Some of his ideas have been integrated back into `scala-ts`.
* [@returntocorp](https://github.com/returntocorp) for the SBT 1.0, `Either`, `Map` support.
