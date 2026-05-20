# Scala-TS Setup Skill (Shared Core Spec)

This file is the shared skill contract used by both Copilot and Claude wrappers.

## Goal

Guide a user to set up Scala-TS safely in an SBT codebase by:

1. detecting current state,
2. confirming understanding with the user,
3. asking targeted setup questions per SBT module,
4. proposing edits (no auto-apply),
5. verifying with `test:compile` and generated-file checks.

## Hard requirements

1. Detect SBT build first (`build.sbt` and/or `project/*.sbt`).
2. If SBT build is not found, warn and stop (no proposal).
3. Before asking any setup question, run preflight `sbt test:compile`; if it fails, report the baseline failure and stop.
4. Never apply edits automatically; propose patches only after explicit user confirmation.
5. For multi-module projects, ask module-scoped generator/config questions for each declared project.
6. After accepted edits, run `sbt test:compile` and verify expected generated files exist.

## Brownfield discovery checklist

1. Parse current modules/projects from `build.sbt`.
2. Detect existing Scala-TS plugin usage:
   - `sbt-scala-ts` / `ScalatsGeneratorPlugin`
   - `sbt-scala-ts-idtlt` / `ScalatsIdtltPlugin`
   - `sbt-scala-ts-python` / `ScalatsPythonPlugin`
3. Detect related config:
   - `scalatsUnionWithLiteral`
   - `scalatsOptionToNullable`
   - `scalatsOnCompile / sourceManaged`
   - `scalatsSingleFilePrinter(...)`
   - `scalatsTypeNaming`, `scalatsFieldMapper`, custom mappers
   - `scalatsPythonBaseModule`
4. Show detected state to the user and ask confirmation before recommendation.

## Per-module question flow

For each SBT module in scope:

1. Generator: TypeScript base, idtlt, Python, or none.
2. `scalatsOnCompile` behavior (default true unless user wants manual trigger).
3. Printer mode (TypeScript modules): file-per-type or single-file printer.
4. Output directory (`scalatsOnCompile / sourceManaged`) default/custom.
5. Mapping preferences where applicable:
   - `Option`: undefined vs nullable (`scalatsOptionToNullable`)
   - sealed traits: default inheritance vs `scalatsUnionWithLiteral`
   - value class handling (`scalatsValueClassAsTagged` when requested)
6. Source/type filters when needed (`scalatsSource*`, `scalatsType*`).
7. Extension-specific:
   - idtlt imports/prelude expectations
   - Python base module and generated path choices

## Decision matrix (from repo examples)

1. Base TypeScript:
   - `project/plugins.sbt`:
     - `addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % "<ver>")`
   - module `build.sbt`:
     - `enablePlugins(ScalatsGeneratorPlugin)`
2. Enumeratum/literal unions:
   - keep base plugin + `scalatsUnionWithLiteral`
3. idtlt:
   - `project/plugins.sbt`:
     - `addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts-idtlt" % "<ver>")`
   - module `build.sbt`:
     - `enablePlugins(ScalatsIdtltPlugin)`
4. Python:
   - `project/plugins.sbt`:
     - `addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts-python" % "<ver>")`
   - module `build.sbt`:
     - `enablePlugins(ScalatsPythonPlugin)`
     - optional `scalatsPythonBaseModule := Some("<module>")`

Reference fixtures:

- `sbt-plugin/src/sbt-test/sbt-scala-ts/simple`
- `sbt-plugin/src/sbt-test/sbt-scala-ts/enumeratum`
- `sbt-plugin-idtlt/src/sbt-test/sbt-scala-ts-idtlt/full`
- `sbt-plugin-python/src/sbt-test/sbt-scala-ts-python/full`
- `sbt-plugin/src/sbt-test/sbt-scala-ts/multi`

## Output contract

Always emit:

1. Detected setup summary.
2. Preflight baseline report (`sbt test:compile` before questions).
3. Confirmed user choices (module by module).
4. Proposed edits grouped by file.
5. Rationale per edit.
6. Explicit confirmation prompt.
7. Post-apply verification report:
   - `sbt test:compile` status
   - expected generated `.ts`/`.py` files present per configured module.

## Distribution policy

1. Mandatory baseline: publish and version skill artifacts in this Git repository.
2. Optional: if a supported Copilot/Claude marketplace/registry exists, package an additional publish path without replacing Git-based distribution.
