# Acceptance Criteria Rules

Acceptance criteria must be:
- Specific
- Measurable
- Observable
- Binary (`PASS` or `FAIL`)

## Good Criterion Pattern
- AC-001: Given [context], when [action], then [observable result].

## Avoid
- Vague wording (`works well`, `fast enough`, `better UX`) without metric.
- Criteria that cannot be verified by any test or observable check.

## Minimum Rule Per Task
- Each functional requirement must map to at least one criterion.
- Each criterion must map to at least one test (or explicit manual check).

## Escalation Rule
If a criterion cannot be made measurable with available context:
1. Flag it as unclear.
2. Ask the human to clarify expected behavior/metric.
3. Do not mark task complete until resolved or explicitly waived.

