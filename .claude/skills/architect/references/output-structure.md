# Output Structure (Architect)

Use this structure for consistent architecture deliverables.

## 1. Context
- What changed.
- Why now.
- Constraints.
- Scope/requirements source: `docs/PROJECT_SPECS.md`.

## 2. Current State
- Existing architecture summary.
- Main bottlenecks/risks.

## 3. Options
- Option A/B/C with pros and cons.
- Why rejected options were rejected.

## 4. Target Architecture
- Boundaries and responsibilities.
- Main interfaces/contracts.
- Cross-cutting concerns (security, observability, resilience).
- Directory layout for source modules under `src/` (or `*/src/` in monorepo).

## 5. Migration Plan
- Incremental steps.
- Dependencies and ordering.
- Rollback strategy.

## 6. Validation
- Acceptance criteria.
- Test and verification strategy.

## 7. Documentation Updates
- `docs/PROJECT_SPECS.md`
- `docs/architecture.md`
- `docs/decisions.md`
- `docs/api-contracts.md` (if applicable)
- `memory-system/task-docs/*` (if task-driven)
