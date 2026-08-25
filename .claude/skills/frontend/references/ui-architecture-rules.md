# UI Architecture Rules

## Component Boundaries
- Keep container/presentation responsibilities explicit.
- Co-locate component-specific logic when possible.
- Extract shared behavior only after duplication is real.

## State Management
- Prefer local state first.
- Promote to shared/global state only when multiple domains need it.
- Keep derived state computed, not duplicated.

## Data Fetching
- Isolate API calls in dedicated client modules.
- Normalize error/loading states.
- Do not couple rendering logic to transport details.

## Styling
- Follow project design system/tokens if available.
- Avoid ad-hoc one-off styles unless task-specific and justified.
- Preserve visual consistency across related screens.
