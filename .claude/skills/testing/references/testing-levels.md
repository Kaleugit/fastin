# Testing Levels (Risk-Based)

Use the minimum set that gives confidence.

## Unit
Use when:
- Business rules and pure logic can be isolated.
- Edge cases and branches are important.

Goal:
- Fast validation of core behavior.

## Integration
Use when:
- Multiple components/services/contracts interact.
- Repositories, APIs, queues, or DB paths are involved.

Goal:
- Validate boundaries and contract compatibility.

## End-to-End (E2E)
Use when:
- Critical user journeys must be validated as a whole.
- Regressions in orchestration flow are likely.

Goal:
- Validate system behavior from user/API entry to final outcome.

## Manual/Exploratory
Use when:
- UX, visual behavior, or environment-dependent behavior is relevant.
- Fast human validation is needed to de-risk release.

Goal:
- Catch gaps not covered by automated checks.

## Selection Rule
- Low risk: unit + minimal integration.
- Medium risk: unit + integration.
- High risk/critical path: unit + integration + e2e (and manual if applicable).

