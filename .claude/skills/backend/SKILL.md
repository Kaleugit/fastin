---
name: backend
description: Use this skill when implementing or refactoring backend/server-side systems, including APIs, business logic, data access, migrations, async jobs, external integrations, reliability hardening, and performance tuning. Use for feature delivery, bug fixes, and backend architecture execution.
user-invocable: false
metadata:
  kind: persona
---

# Backend Skill

This skill standardizes backend delivery for this boilerplate with a root-cause and contract-first mindset.

## When To Use
- When implementing or refactoring backend systems, APIs, business logic, or data access.

## Additional Inputs To Read
Assume baseline context from `CLAUDE.md` is already loaded.

1. `docs/PROJECT.md` (mandatory, source of truth — modelo de dados)
2. `docs/architecture.md`
3. `docs/decisions.md`

## Workflow
1. Clarify requirements, constraints, and acceptance criteria.
2. Define/confirm contracts and data implications before implementation.
3. Implement business logic with explicit boundaries.
4. Add/update data layer and migrations safely.
5. Integrate external services with retries/timeouts/idempotency where needed.
6. Validate behavior with a estratégia em `.claude/skills/testing/SKILL.md`.
7. Registre a evidência de aceitação na etapa entregue ao humano.

## Mandatory Rules
- Código de produção fica em `app/src/main/java/dev/kaleu/fastin/`. Testes unitários em `app/src/test/`, instrumentados em `app/src/androidTest/`.
- Fix root cause; no workaround as final solution.
- Keep APIs and data contracts explicit and version-aware.
- If expected behavior, contract semantics, or test scope is ambiguous, ask the human.

## Required Outputs
- Backend changes implemented under `app/src/main/java/dev/kaleu/fastin/`.
- Contract/data impacts documented when applicable.
- Acceptance criteria status with evidence (`PASS`/`FAIL`).

## Quality Bar
- Predictable error semantics and observability baseline.
- Safe data changes with rollback awareness.
- No hidden coupling between modules/services.
- No test manipulation to force green results.

## Reference Files
- Backend delivery checklist: `references/backend-delivery-checklist.md`
- API contract rules: `references/api-contract-rules.md`
- Data and migrations guide: `references/data-and-migrations.md`
- Reliability baseline: `references/reliability-baseline.md`
