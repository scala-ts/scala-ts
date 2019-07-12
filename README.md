# scala-ts

*scala-ts* is a simple tool which can generate TypeScript interfaces and classes from Scala case classes.

It's helpful when working with REST-ful Scala backend and TypeScript frontend. Having defined Scala types returned by your endpoints you can easily generate TypeScript definitions for consuming these endpoints.

> See [*Scala-ts: Scala to TypeScript code generator*](http://codewithstyle.info/scala-ts-scala-typescript-code-generator/) at Code with Style.

## Usage

*scala-ts* can be used either standalone or as a sbt plugin.

See:

- [Usage details](../docs/index.html#usage)
- [Releases](https://github.com/scala-ts/scala-ts/releases) (with downloads)

## Build manually

It can be built from this source repository.

    sbt +publishLocal

To run the tests, use:

    sbt test

[Travis](https://travis-ci.org/scala-ts/scala-ts): ![Travis build status](https://travis-ci.org/scala-ts/scala-ts.svg?branch=master)

## Credits

Many thanks to:

* https://github.com/nicolasdalsass who forked the project into https://github.com/Elium/scala-ts/tree/master. I incorporated some of his ideas into `scala-ts`.
* https://github.com/returntocorp - for SBT 1.0 support, Either, Map

