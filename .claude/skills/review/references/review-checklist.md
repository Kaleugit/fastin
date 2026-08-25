# Review Checklist

Detailed checklist for technical code review. Apply to every changeset.

## 1. Correctness
- [ ] Logic matches the stated intent (task planning, PR description).
- [ ] Edge cases are handled (nulls, empty collections, boundary values).
- [ ] Error paths return meaningful feedback, not silent failures.
- [ ] State mutations are intentional and documented.

## 2. API & Contract Integrity
- [ ] Public API signatures are backward-compatible (or breaking change is documented).
- [ ] Request/response schemas match `docs/api-contracts.md`.
- [ ] Database schema changes have corresponding migrations.
- [ ] Event/message contracts are versioned if consumed externally.

## 3. Security Surface
- [ ] User input is validated and sanitized at system boundaries.
- [ ] Authentication and authorization checks are present where required.
- [ ] No secrets, tokens, or credentials in code or config files.
- [ ] Dependencies with known vulnerabilities are flagged.

## 4. Regression Vectors
- [ ] Existing tests still pass without modification (or modifications are justified).
- [ ] Behavioral changes in shared modules are traced to all consumers.
- [ ] Feature flags or conditional paths don't leave orphaned code.
- [ ] Rollback path exists for data migrations.

## 5. Performance
- [ ] No unbounded queries or N+1 patterns in data access.
- [ ] Hot paths avoid unnecessary allocations or I/O.
- [ ] Caching changes don't introduce staleness or inconsistency.
- [ ] Pagination is used for list endpoints.

## 6. Observability
- [ ] Error paths have logging with sufficient context.
- [ ] Critical operations emit metrics or traces.
- [ ] Log levels are appropriate (no sensitive data in logs).

## 7. Test Quality
- [ ] New behavior has corresponding test coverage.
- [ ] Tests verify behavior, not implementation details.
- [ ] No test manipulation (disabled assertions, forced mocks, skipped tests).
- [ ] Test names describe the scenario, not the method.

## 8. Code Quality
- [ ] Follows project patterns from `docs/patterns.md`.
- [ ] No unnecessary duplication (DRY within reason).
- [ ] Naming is clear and consistent with codebase conventions.
- [ ] Complexity is proportional to the problem being solved.
