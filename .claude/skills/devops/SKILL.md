---
name: devops
description: Use this skill when designing or updating CI/CD pipelines, build and deploy automation, container/runtime configuration, infrastructure as code workflows, observability baselines, release/rollback plans, and operational reliability checks.
user-invocable: false
metadata:
  kind: persona
---

# DevOps Skill

This skill standardizes delivery and operations changes with release safety and operational clarity.

## When To Use
- When designing or updating CI/CD, deployment, infrastructure, or operational reliability.

## Additional Inputs To Read
Assume baseline context from `CLAUDE.md` is already loaded.

1. `docs/PROJECT.md`
2. `docs/build-apk.md` (procedimento de build e sideload)
3. `gradle/libs.versions.toml`

## Workflow
1. Define target environment, constraints, and rollout expectations.
2. Plan build/deploy pipeline changes with rollback path.
3. Apply infra/runtime changes incrementally.
4. Validate deployability, health checks, and failure behavior.
5. Validate observability baseline (logs/metrics/alerts).
6. Record evidence, operational notes, and residual risks.

## Mandatory Rules
- Código de produção fica em `app/src/main/java/dev/kaleu/fastin/`. Testes unitários em `app/src/test/`, instrumentados em `app/src/androidTest/`.
- Prefer simple deploy pipelines before advanced orchestration.
- No release without rollback strategy when risk is medium/high.
- If SLO/release criteria are unclear, ask the human before finalizing.

## Required Outputs
- Pipeline/deployment changes documented.
- Rollout and rollback steps explicit.
- Acceptance criteria status with evidence (`PASS`/`FAIL`).
- Updated task planning/report artifacts when task-driven.

## Quality Bar
- Build, test, and deploy flow is deterministic.
- Runtime has minimum observability and health signals.
- Operational risk and runbook steps are explicit.

## Reference Files
- Delivery pipeline checklist: `references/delivery-pipeline-checklist.md`
- Deployment and rollback strategy: `references/deployment-and-rollback.md`
- Observability baseline: `references/observability-baseline.md`
- Runbook template: `references/runbook-template.md`
- Governance CI modes (`strict`/`lite`/`off`): `references/ci-modes.md`
