# Regression Analysis Guide

How to identify and assess regression risk in a changeset.

## What Is a Regression Vector?

A regression vector is a path through which a code change can cause
previously working behavior to fail. It is not hypothetical — it must
be traceable to specific existing functionality.

## Analysis Steps

### 1. Map the blast radius
- List every file modified in the changeset.
- For each file, identify its consumers (imports, API callers, event listeners).
- Trace transitive dependencies up to 2 levels.

### 2. Identify behavioral changes
- Diff each file and classify changes as:
  - **Additive**: new code that doesn't touch existing paths.
  - **Modifying**: changes to existing logic, signatures, or data flow.
  - **Removing**: deleted code, removed features, deprecated paths.
- Focus regression analysis on Modifying and Removing changes.

### 3. Assess impact per vector

For each regression vector, assess:

| Factor        | Question                                              |
|---------------|-------------------------------------------------------|
| Probability   | How likely is this to cause a failure?                |
| Blast radius  | How many users/features are affected if it fails?     |
| Detectability | Will existing tests or monitoring catch it?           |
| Reversibility | Can the change be rolled back quickly?                |

### 4. Risk rating

Combine the factors into a risk rating:

- **High risk**: Modifying change in shared module, low test coverage,
  wide blast radius. Requires explicit test evidence before merge.
- **Medium risk**: Modifying change with good test coverage but wide
  blast radius. Proceed with monitoring plan.
- **Low risk**: Additive change, isolated module, strong test coverage.
  Standard review sufficient.

## Common Regression Patterns

1. **Signature change in shared utility** — all callers must be updated.
2. **Default value change** — silent behavioral shift for existing consumers.
3. **Query modification** — data shape or volume changes downstream.
4. **Middleware/interceptor change** — affects all routes passing through.
5. **Config/env variable rename** — breaks deployments that haven't updated.
6. **Dependency upgrade** — transitive API changes not covered by types.

## Output Format

```
## Regression Risk Summary

### High Risk
- [File:line] Description of vector. Affected consumers: X, Y.
  Mitigation: <test evidence or required action>.

### Medium Risk
- [File:line] Description. Monitoring plan: <what to watch>.

### Low Risk
- [File:line] Description. Covered by existing tests.

### No Regression Vectors Found
- Additive-only change, isolated scope.
```
