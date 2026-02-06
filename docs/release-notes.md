---
layout: default
---

# Release notes

[![Maven](https://img.shields.io/maven-central/v/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22scala-ts-core_{{site.scala_major_version}}%22)

[*Version 0.8.1*]https://github.com/scala-ts/scala-ts/compare/0.8.0..0.8.1) - Improve `Map` & `Set` support for better inference in generated TypeScript.

[*Version 0.8.0*](https://github.com/scala-ts/scala-ts/compare/0.7.1..0.8.0) - Support tuples as invariants, and fix for aggregate build.

[*Version 0.7.0*](https://github.com/scala-ts/scala-ts/compare/0.6.0..0.7.0) - Support composite type aka different kinds of type with same name (e.g. Scala class and its companion object); `Map` Typescript mapping updated from `Partial<Record<K, V>>` to `Readonly<Map<K, V>>` to support non-literal type as keys.

[*Version 0.6.0*](https://github.com/scala-ts/scala-ts/compare/0.5.19..0.6.0) - Improve IDTLT enum declaration mappier

[*Version 0.5.19*](https://github.com/scala-ts/scala-ts/compare/0.5.18..0.5.19) - Update idtlt

[*Version 0.5.18*](https://github.com/scala-ts/scala-ts/compare/0.5.17..0.5.18) - Improve TypeScript mappings (`Record`).

[*Version 0.5.17*](https://github.com/scala-ts/scala-ts/compare/0.5.13..0.5.17) - Improve logging, tagged types and singletons

[*Version 0.5.13*](https://github.com/scala-ts/scala-ts/compare/0.5.12..0.5.13) - Scala 3 support

[*Version 0.5.12*](https://github.com/scala-ts/scala-ts/compare/0.5.11..0.5.12) - Improve invariant import resolution.

[*Version 0.5.11*](https://github.com/scala-ts/scala-ts/compare/0.5.10..0.5.11) - Improve invariant parsing.

[*Version 0.5.10*](https://github.com/scala-ts/scala-ts/compare/0.5.9..0.5.10) - Better handling of `Map` as invariants, supporting Value class as keys.

[*Version 0.5.9*](https://github.com/scala-ts/scala-ts/compare/0.5.8...0.5.9) - Minor improvements, and better management of invariants (constants in singletons; see [examples](./examples.html#example-9)).

[*Version 0.5.8*](https://github.com/scala-ts/scala-ts/compare/0.5.7...0.5.8) - Optionally emit Value classes as tagged types.

[*Version 0.5.7*](https://github.com/scala-ts/scala-ts/compare/0.5.6...0.5.7) - Provides array of known values for union types; Generate type guards.

[*Version 0.5.6*](https://github.com/scala-ts/scala-ts/compare/0.5.5...0.5.6) - Improve [idonttrustlikethat](https://scala-ts.github.io/scala-ts/#idonttrustlikethat); Discriminated factory; Constant for Enum values; Better support for `Map` as dictionary.

[*Version 0.5.5*](https://github.com/scala-ts/scala-ts/compare/0.5.4...0.5.5) - Patch version; Fix compilation order (in case of forward declaration).

[*Version 0.5.4*](https://github.com/scala-ts/scala-ts/compare/0.5.3...0.5.4) - Patch version; Fix cross compilation with idtlt SBT plugin.

*Version 0.5.3* - Patch version; Fix dependencies.

[*Version 0.5.2*](https://github.com/scala-ts/scala-ts/compare/0.5.1...0.5.2) - Patch version; Fix duplicate TypeScript output on Scala re-compilation (+ increase test coverage).

[*Version 0.5.1*](https://github.com/scala-ts/scala-ts/compare/0.5.0...0.5.1) - Patch version; Refactor `import` as `import type`, and fix import resolution for `Enumeration` field.

[**Version 0.5.0**](https://www.linkedin.com/posts/cchantep_sbt-typescript-scala-activity-6760550250210979840-g0xr) - Major version; Full refactoring to provide easy to use, but yet highly configurable & extensible compiler and SBT plugins to generate TypeScript types from Scala compiled one.

*Version 0.3.2* - added support for more types; added file output support.

*Version 0.4.0* - added support for SBT 1.0, `Either` and `Map`.
