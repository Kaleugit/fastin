# Integrity Anti-Patterns (Testing)

Use this file to identify behaviors that seem to solve the issue but degrade quality.

## 1. Test Manipulation
Anti-pattern:
- Change test expectations to fit broken implementation.
- Comment out critical assertions.
- Create a "for test only" function in the main flow.

Correct pattern:
- Fix the real implementation.
- Keep tests as specification of expected behavior.

## 2. Symptom Masking
Anti-pattern:
- Catch an error and return fake success.
- Replace real error with silent fallback.

Correct pattern:
- Investigate root cause (configuration, dependency, contract, concurrency).
- Fix error origin and preserve proper error semantics.

## 3. Quick Hack Disguised As Fix
Anti-pattern:
- Hardcode around a broken flow.
- Remove edge case to simplify artificially.
- Reduce requirement without human approval.

Correct pattern:
- Break problem into smaller steps.
- Implement complete solution for approved requirements.

## 4. Flaky Test Avoidance
Anti-pattern:
- Increase timeout without diagnosis.
- Re-run repeatedly and accept best result.
- Mark test as ignorable in CI.

Correct pattern:
- Identify source of non-determinism.
- Eliminate race condition, unstable external dependency, or implicit order.

## 5. Integrity Decision Rule
Mandatory questions:
1. Does this change fix root cause?
2. Does the test validate real production behavior?
3. Is there objective evidence for each `PASS`/`FAIL` criterion?

If any answer is "no" or "I don't know", pause and escalate.
