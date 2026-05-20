---
description: Analyze this SBT repository and prepare Scala-TS setup proposals module by module
---

Follow the shared skill contract in `docs/ai/scala-ts-setup-core.md`.

Execution rules:

1. Detect SBT first; if missing, stop with a clear warning.
2. Run preflight `sbt test:compile` before any question; if it fails, report baseline failure and stop.
3. Discover and summarize current Scala-TS-related configuration.
4. Ask for confirmation of discovered state before proposing changes.
5. Ask module-specific setup questions for each declared SBT project.
6. Propose edits only; do not apply changes without explicit confirmation.
7. After accepted edits, run `sbt test:compile`.
8. Verify generated TypeScript/Python files exist as expected for each configured module.
9. Return:
   - detected state,
   - preflight baseline result,
   - chosen options by module,
   - file-by-file proposed edits,
   - verification result.
