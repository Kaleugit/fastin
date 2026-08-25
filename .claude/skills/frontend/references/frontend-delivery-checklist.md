# Frontend Delivery Checklist

Use before marking frontend work complete.

## Scope And Contracts
- Acceptance criteria are explicit and testable.
- API contract expected by the UI is documented.
- Component ownership is clear.

## UX And Accessibility
- Primary flow works with keyboard.
- Inputs have proper labels and error feedback.
- Focus order is predictable.
- Contrast/readability baseline is acceptable.

## Responsiveness And Performance
- Main screens work on target breakpoints.
- Avoid unnecessary re-renders/state duplication.
- Heavy assets are optimized/lazy-loaded when relevant.

## Quality And Integrity
- Source files are under `src/` (or `*/src/` in monorepo).
- No mocked UI behavior hiding real defects.
- Acceptance criteria status has evidence (`PASS`/`FAIL`).
