---
name: architect
description: Use this skill when you need to design, review, or evolve software architecture, define technical boundaries/contracts, or produce architecture decisions and migration plans in this repository.
user-invocable: false
metadata:
  kind: persona
---

# Architect Skill

This skill is for architecture work in this boilerplate and should be applied when the task involves system design, architectural trade-offs, service boundaries, API/interface contracts, or architecture documentation updates.

## When To Use
- New feature with cross-module impact.
- Refactor that changes boundaries or dependencies.
- Performance/scalability/reliability/security concerns at system level.
- Need to define or revise architecture documents and ADRs.

## Additional Inputs To Read
Assume baseline context from `CLAUDE.md` is already loaded.

1. `docs/PROJECT.md` (mandatory, source of truth — escopo, modelo de dados, telas)
2. `docs/architecture.md`
3. `docs/decisions.md`
4. `docs/design-system.md` (quando a decisão afeta UI)
5. `design-ref/` (referências visuais originais)

## Workflow
1. Clarify scope and constraints.
2. Map current architecture and pain points.
3. Generate 2-3 viable options with trade-offs.
4. Choose a target approach aligned with KISS/YAGNI.
5. Define boundaries, interfaces, and migration steps.
6. Atualize `docs/architecture.md` e `docs/decisions.md`.

## Required Outputs
- Architecture update in `docs/architecture.md` (or a focused section update).
- Decision record in `docs/decisions.md` for non-trivial choices.
- API/interface impacts in `docs/api-contracts.md` when applicable.

## Mandatory Rules

## Quality Bar
- Explicit trade-offs (pros/cons and rejected alternatives).
- Compatibility and migration/rollback considerations when relevant.
- Clear, testable acceptance criteria for downstream implementation.
- No workaround-oriented design; root-cause-oriented decisions only.
- Código de produção respeita a convenção `app/src/main/java/dev/kaleu/fastin/`.

## Reference Files
- For architecture review checklist, read `references/review-checklist.md`.
- For expected artifact structure, read `references/output-structure.md`.
