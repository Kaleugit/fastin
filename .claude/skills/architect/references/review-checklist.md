# Architecture Review Checklist

Use this checklist before finalizing architecture changes.

## Scope And Boundaries
- Problem statement is explicit and measurable.
- Scope is aligned with `docs/PROJECT_SPECS.md`.
- Module/service boundaries are clear.
- Ownership and dependency directions are clear.
- Source layout follows `src/` convention (or `*/src/` in monorepo).

## Trade-Offs
- At least one alternative was considered.
- Chosen option has rationale.
- Cost/complexity impact is documented.

## NFR Coverage
- Performance implications identified.
- Scalability implications identified.
- Reliability/failure modes identified.
- Security/privacy implications identified.
- Operability/observability implications identified.

## Contracts And Data
- API/interface contracts are aligned with architecture.
- Data flow and persistence impacts are documented.
- Backward compatibility is addressed (if needed).

## Delivery Readiness
- Migration plan is defined (if needed).
- Rollback strategy is defined (if needed).
- Test strategy impact is explicit.
- Documentation files are updated consistently.
