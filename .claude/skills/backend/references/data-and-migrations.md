# Data And Migrations

## Data Modeling
- Model around domain behavior, not convenience fields.
- Keep invariants enforced in schema and application layers.
- Prefer explicit constraints/indexes over implicit assumptions.

## Migration Strategy
- Write deterministic forward migrations.
- Define rollback strategy when feasible.
- Plan for large-table changes to avoid lock/regression risks.

## Query And Performance
- Index for real query patterns.
- Avoid N+1 and unbounded scans on critical paths.
- Measure before and after tuning.

## Safety
- Validate migration effects in non-prod before rollout.
- Capture operational notes in task report when risk is medium/high.
