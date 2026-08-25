---
name: review
description: >
  Use this skill when you need to perform technical code review, assess
  regression risk, evaluate change impact, or produce a severity-ranked
  checklist of findings before merge or delivery.
user-invocable: false
metadata:
  kind: persona
---

# Review Skill

This skill standardizes technical review for this boilerplate with a
risk-based, severity-ranked approach. It covers code quality, regression
detection, architectural impact, and delivery readiness.

## When To Use
- Pull request or changeset needs technical review.
- Pre-merge regression risk assessment.
- Post-implementation quality gate before delivery.
- Cross-cutting change that may introduce unintended side effects.
- Refactor or migration that requires impact analysis.

## Additional Inputs To Read
Assume baseline context from `CLAUDE.md` is already loaded.

1. `docs/PROJECT.md`
2. `docs/architecture.md`
3. `docs/decisions.md`
4. `docs/design-system.md` (quando a mudança afeta UI)

## Workflow
1. Understand the intent: read task planning, PR description, or commit messages.
2. Map the change surface: files touched, modules affected, dependency graph.
3. Run the severity checklist (see below) against every changed file/module.
4. Identify regression vectors: what existing behavior could break.
5. Cross-reference with architecture docs and API contracts.
6. Produce findings report ranked by severity.
7. Recommend actions: approve, request changes, or escalate.

## Severity Checklist

Every review must evaluate findings against these severity tiers.
Each finding gets exactly one severity level.

### Critical — Must block merge
- Data loss or corruption risk.
- Security vulnerability (injection, auth bypass, secret exposure).
- Breaking change to public API without migration path.
- Race condition or deadlock in concurrent code.
- Deployment blocker (missing migration, env variable, infra dependency).

### High — Should block merge unless explicitly accepted
- Regression in existing functionality (behavioral change without test update).
- Contract violation (API, data schema, interface mismatch).
- Missing error handling on external boundaries (I/O, network, user input).
- Performance degradation in hot path (unbounded queries, N+1, missing index).
- Test coverage gap for critical business logic.

### Medium — Should be fixed, may proceed with tracking
- Code duplication that increases maintenance burden.
- Inconsistency with established project patterns or conventions.
- Missing or misleading documentation for non-obvious behavior.
- Observability gap (no logging/metrics for failure paths).
- Incomplete cleanup of deprecated code or feature flags.

### Low — Improvement opportunity, does not block
- Style or naming inconsistency within scope of change.
- Minor optimization opportunity.
- TODO/FIXME without tracking issue.
- Test readability improvement.
- Non-blocking suggestion for alternative approach.

## Mandatory Rules
- Never approve a change with unresolved Critical findings.
- Never downgrade severity to avoid blocking; escalate instead.
- Review the tests changed alongside the code — test manipulation to force
  green is a Critical finding.
- If the change affects API contracts, verify backward compatibility or
  explicit breaking-change documentation.
- If risk assessment is ambiguous, ask the human before approving.

## Required Outputs
- Findings list with severity (`critical`/`high`/`medium`/`low`).
- Regression risk summary: what could break and likelihood.
- Verdict: `APPROVE`, `REQUEST_CHANGES`, or `ESCALATE`.
- For each Critical/High finding: specific remediation recommendation.
- Updated task artifacts when operating within a task context.

## Quality Bar
- Every changed file is reviewed — no silent pass-throughs.
- Findings are actionable with clear location (file:line when possible).
- Severity assignment is justified, not arbitrary.
- Regression vectors are traced to specific existing behavior, not hypothetical.
- No rubber-stamp approvals; even clean changes get an explicit "no findings" note.

## Reference Files
- Review checklist detail: `references/review-checklist.md`
- Regression analysis guide: `references/regression-analysis.md`
- Severity classification guide: `references/severity-guide.md`
