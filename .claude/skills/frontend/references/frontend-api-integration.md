# Frontend API Integration Rules

## Contract First
- Confirm endpoint contract before implementation.
- Map API schema to UI model explicitly.
- Handle nullable/optional fields deliberately.

## Error Semantics
- Distinguish validation, auth, and server errors in UI.
- Surface user-safe messages and keep technical details in logs.
- Do not convert backend failures into fake success states.

## Resilience
- Guard against duplicate submissions.
- Use cancellation/debounce where user interaction can race.
- Handle slow/loading states explicitly.

## Testing Focus
- Validate happy path + critical failure paths.
- Validate state transitions for loading/success/error.
- Keep evidence linked to acceptance criteria.
