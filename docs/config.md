---
layout: default
---

# Configuration

*Scala-TS* can be configured in many ways.

## Compiler settings

- `optionToNullable` - Translate `Option` types to union type with `null` (e.g. `Option[Int]` to `number | null`); Default `false` as builtin behaviour is option-to-undefined (see [`TypeScriptTypeMapper.NullableAsOption`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptTypeMapper$$NullableAsOption.html)).
- `prependEnclosingClassNames` - Prepend the name of enclosing classes to the generated types (default: `true`)
- `typescriptIndent` - The characters used as TypeScript indentation (default: 2 spaces).
- `typescriptLineSeparator` - The characters used to separate TypeScript line/statements (default: `;`).
- `typeNaming` - The conversions for the type names (class implementing [`TypeScriptTypeNaming`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}/io/github/scalats/core/TypeScriptTypeNaming.html); default: [`Identity`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}/io/github/scalats/core/TypeScriptTypeNaming$$Identity$.html)). *See: [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/custom-cfg/build.sbt#L13)*
- `fieldMapper` - The conversions for the field names (implements [`TypeScriptFieldMapper`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptFieldMapper.html); default: [`Identity`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptFieldMapper$$Identity$.html)). *See: [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/custom-cfg/build.sbt#L16)*
- `discriminator` - The name of the field to be used as discriminator (default: `_type`).

The Scala code base to be considered to generate TypeScript from can be filtered using the following build options.

- `compilationRuleSet` - Set of rules to specify which Scala source files must be considered.
- `typeRuleSet` - Set of rules to specify which types (from the already filtered source files) must be considered.

A rule set such as `compilationRuleSet` is described with multiple include and/or excludes rules:

```
compilationRuleSet {
   includes = [ "ScalaParserSpec\\.scala", "Transpiler.*" ]
   excludes = [ "foo" ]
}

typeRuleSet {
  # Regular expressions on type full names.
  # Can be prefixed with either 'object:' or 'class:' (for class or trait).
  includes = [ "org\\.scalats\\.core\\..*" ]

  excludes = [
    ".*Spec", 
    "ScalaRuntimeFixtures$", 
    "object:.*ScalaParserResults", 
    "FamilyMember(2|3)"
  ]
}
```

Also the settings can be used from advanced configuration.

- `printer` - An optional printer class (implements [`TypeScriptPrinter`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptPrinter.html)). *See: [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/custom-cfg/build.sbt#L26)*
- `typeScriptTypeMappers` - A list of type mappers (implements [`TypeScriptTypeMapper`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptTypeMapper.html)).
- `typeScriptImportResolvers` - A list of import resolvers (implements [`TypeScriptImportResolver`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptImportResolver.html)).
- `typeScriptDeclarationMappers` - A list of declaration mappers (implements [`TypeScriptDeclarationMapper`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptDeclarationMapper.html)). Some additional declaration mappers are provided: [`enumerationAsEnum`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptDeclarationMapper$.html#enumerationAsEnum:io.github.scalats.core.TypeScriptDeclarationMapper.EnumerationAsEnum), [`singletonAsLiteral`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptDeclarationMapper$.html#singletonAsLiteral:io.github.scalats.core.TypeScriptDeclarationMapper.SingletonAsLiteral), [`scalatsUnionAsSimpleUnion`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/TypeScriptDeclarationMapper$.html#unionAsSimpleUnion:io.github.scalats.core.TypeScriptDeclarationMapper.UnionAsSimpleUnion), `scalatsUnionWithLiteral`.
- `additionalClasspath` - A list of URL to be added to the plugin classpath (to be able to load `fieldNaming` or `printer` ...).

[![javadoc](https://javadoc.io/badge2/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}/javadoc.svg)](https://javadoc.io/doc/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}) 

## SBT plugin settings

The [compiler plugin settings](#compiler-settings) can be configured as SBT settings, using the `scalats` prefix; e.g. The `scalatsTypescriptIndent` SBT setting corresponds to the compiler plugin setting `typescriptIndent`.

```ocaml
scalatsTypescriptIndent := "\t"
```

The SBT plugin also has some specific settings.

- `scalatsOnCompile` - Boolean setting that if true triggers the TypeScript generation on Scala compilation (default: `true`).
- `scalatsDebug` - Boolean setting to enable debug for the SBT plugin (default: `false`)
- `sourceManaged in scalatsOnCompile` - The directory to initialize the printer with, where to generate the TypeScript code (default: `target/scala-ts/src_managed`).
- `scalatsPrinterPrelude` - The prelude content to be printed at the beginning of each generated TypeScript file (default: `// Generated by ScalaTS ...`).

The following utilities are provided to ease setting up the printer prelude.

- `scalatsPrinterInMemoryPrelude(lines: Strings*)` - Set prelude from embedded lines (see [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/custom-cfg/build.sbt#L28))
- `scalatsPrinterUrlPrelude(url)` - Set prelude from URL (see [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/single-file-printer/build.sbt#L11))

```ocaml
// In memory prelude
scalatsPrinterPrelude := scalatsPrinterInMemoryPrelude(
  "line 1",
  "line 2",
  "...",
  "line N")

// Or, from file/URL
scalatsPrinterPrelude := scalatsPrinterUrlPrelude(
  (baseDirectory.value / "project" / "prelude.ts").toURI.toURL)
```

More than the prelude, the TypeScript printer can be further customized through the `scalatsPrinter` settings, with the utilities provided along the SBT plugin.

- `scalatsFilePrinter` - Print one file per type.
- `scalatsSingleFilePrinter` - Print all types in a single file (see [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/single-file-printer/build.sbt#L9).
- `scalatsPrinterForClass(props*)` - Use a custom printer (see [example](https://github.com/scala-ts/scala-ts/blob/master/sbt-plugin/src/sbt-test/sbt-scala-ts/custom-cfg/build.sbt#L25)).

```ocaml
scalatsPrinter := scalatsSingleFilePrinter

scalatsPrinter := scalatsSingleFilePrinter // Default single file 'scala.ts'

scalatsPrinter := scalatsPrinterForClass[CustomPrinter]()
```

Optionally the following argument can be passed.

- `-P:scalats:debug` - Enable debug.
- `-P:scalats:printerOutputDirectory=/path/to/base` - Path to a base directory to initialize a custom printer with.
- `-P:scalats.sys.scala-ts.single-filename=filename.ts` - Set the filename if using the `SingleFilePrinter`.
- `-P:scalats:sys.scala-ts.printer.prelude-url=/path/to/prelude` - Set the system property (`scala-ts.printer.prelude-url`) to pass `/path/to/prelude` as printer prelude.
- `-P:scalats:sys.scala-ts.printer.import-pattern=import-pattern` - Override the pattern to print pattern (default: `type { %1$s }` with `%1$s` being the placeholder for the name of the type to be imported).
