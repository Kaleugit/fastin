# Severity Classification Guide

How to assign severity to review findings consistently.

## Decision Tree

```
Is there data loss, security exposure, a public API break without migration path, or a deployment blocker?
  YES → CRITICAL

Does it break existing behavior or violate a contract?
  YES → HIGH

Does it hurt maintainability, observability, or consistency?
  YES → MEDIUM

Is it a style, readability, or minor improvement suggestion?
  YES → LOW
```

## Severity Definitions

### Critical
**Impact**: Immediate harm to users, data, or system availability.
**Action**: Must be resolved before merge. No exceptions.

Examples:
- SQL injection or XSS vulnerability.
- Unprotected endpoint exposing sensitive data.
- Breaking change to a public API without a migration path.
- Migration that drops a column without data backup.
- Race condition in payment or state-critical flow.
- Missing required environment variable for deployment.

### High
**Impact**: Functional regression or contract violation that will
cause failures in production.
**Action**: Should be resolved before merge. May proceed only with
explicit risk acceptance from the task owner.

Examples:
- API response shape changed without version bump.
- Business logic change without corresponding test update.
- Error handling removed from external service call.
- N+1 query in high-traffic endpoint.
- Authentication check missing on new route.

### Medium
**Impact**: Increased maintenance cost, reduced observability, or
deviation from established patterns. Not an immediate production risk.
**Action**: Should be fixed. May merge with a tracking issue created.

Examples:
- Duplicated logic that exists in a shared utility.
- Missing error logging on a failure path.
- Inconsistent naming with project conventions.
- Dead code or unused imports left behind.
- Documentation not updated for behavioral change.

### Low
**Impact**: Code quality improvement opportunity. No functional or
operational risk.
**Action**: Suggestion only. Author decides whether to address.

Examples:
- Variable name could be more descriptive.
- Test assertion message could be clearer.
- Opportunity to use a more idiomatic pattern.
- TODO comment without a tracking issue.
- Minor formatting inconsistency.

## Anti-Patterns in Severity Assignment

- **Inflation**: marking style issues as High to force changes.
  Severity must match actual impact.
- **Deflation**: marking contract violations as Low to avoid blocking.
  If it breaks consumers, it's High minimum.
- **Hypothetical risk**: marking something Critical based on what
  *could* happen in a scenario that doesn't exist in the codebase.
  Findings must be grounded in actual code paths.
- **Severity by volume**: a file with many Low findings doesn't
  become High. Each finding stands on its own.
