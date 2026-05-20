---
mode: ask
description: Analyze an SBT codebase and propose Scala-TS setup edits module by module
---

Use `docs/ai/scala-ts-setup-core.md` as the authoritative contract.

Your task:

1. Detect whether the repository is an SBT build.
2. If no SBT build is found, stop and explain that setup requires SBT files.
3. Before any question, run preflight `sbt test:compile`; if it fails, report baseline failure and stop.
4. Detect current Scala-TS setup (plugins, modules, settings).
5. Confirm your detected setup with the user.
6. Ask per-module setup questions (generator, compile trigger, output path, mapping options).
7. Propose edits only (no auto-apply) in:
   - `project/plugins.sbt`
   - module/root `build.sbt`
8. After user confirmation and edits, run `sbt test:compile`.
9. Verify expected generated files (`.ts` and/or `.py`) exist per configured module.
10. Report results and any remediation suggestions.
