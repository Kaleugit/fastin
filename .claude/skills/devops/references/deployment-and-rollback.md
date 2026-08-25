# Deployment And Rollback Strategy

## Deployment
- Use incremental rollout when possible.
- Validate service health immediately after deploy.
- Stop rollout if critical checks fail.

## Rollback
- Define rollback trigger criteria.
- Define rollback command/procedure.
- Validate recovery after rollback.

## Decision Rule
If deployment risk is unclear or rollback is undefined, pause and ask human.
