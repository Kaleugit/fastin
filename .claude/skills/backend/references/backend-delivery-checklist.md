# Backend Delivery Checklist

Use before marking backend work complete.

## Contracts And Behavior
- Requirement-to-contract mapping is explicit.
- Response/error semantics are consistent.
- Acceptance criteria are measurable and testable.

## Data And Consistency
- Schema/data changes are documented.
- Migration/rollback impact is understood.
- Integrity constraints are preserved.

## Reliability
- Timeouts/retries/circuit logic are appropriate where needed.
- Idempotency is defined for retryable operations.
- Logs/metrics are sufficient for diagnostics.

## Quality And Integrity
- Source files are under `src/` (or `*/src/` in monorepo).
- No workaround or test-only code paths in production flow.
- Acceptance criteria status has evidence (`PASS`/`FAIL`).
