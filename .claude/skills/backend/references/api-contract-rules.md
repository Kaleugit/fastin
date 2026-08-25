# API Contract Rules

## Design
- Define resource/action semantics before coding handlers.
- Keep contract backward compatibility explicit.
- Version only when compatibility cannot be preserved.

## Request And Response
- Validate input at boundaries.
- Return stable response shapes.
- Return explicit machine-readable error codes where relevant.

## Operational Semantics
- Document auth/authorization requirements.
- Document pagination/filter/sorting behavior.
- Document rate limit and retry expectations when applicable.

## Verification
- Map each critical contract behavior to test coverage.
- Keep examples aligned with real implementation.
