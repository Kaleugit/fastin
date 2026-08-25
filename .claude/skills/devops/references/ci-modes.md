# Governance CI Modes

The two governance workflows (`.github/workflows/governance.yml` and
`.github/workflows/finalize-task-metadata.yml`) honor a configurable mode
declared in `.governance/ci-mode.conf` (single line, single token).

This document explains each mode, when to choose it, and how to configure it.
The authoritative decision record is `docs/decisions.md` ADR-010.

## Why this exists
`skills/delivery/scripts/deliver-to-main.sh` already runs
`./scripts/validate-all.sh` locally as a gate before pushing the branch.
Without configurable modes, governance CI re-runs the same validator
remotely on every `pull_request` (open/synchronize) AND every `push: main`,
validating the same HEAD multiple times per delivery. On PRIVATE
repositories this exhausts Actions cota quickly (~5 runs per PR,
~50 runs/epic in observed projects).

## The three modes

### strict
Current pre-change behavior preserved verbatim.

- Governance CI runs on:
  - `pull_request` opened/synchronize against `main`
  - `push: main` (including bot pushes)
- `finalize-task-metadata.yml` runs on every PR merge.

Choose `strict` when:
- The repository is PUBLIC (Actions cota is generous).
- You explicitly want remote redundancy for PR events.
- The project pays for a larger Actions plan.

### lite (DEFAULT)
Applied when `.governance/ci-mode.conf` is absent or empty.

- Governance CI runs only on `push: main` by human actors.
- PR events (open/synchronize) and `github-actions[bot]` pushes
  early-exit with `exit 0` (neutrally green).
- `finalize-task-metadata.yml` keeps current behavior (still runs on merge).

Choose `lite` when:
- The repository is PRIVATE and you follow the standard
  `deliver-to-main.sh` flow (the local validate-before-push gate is in
  place).
- You want a ~80% reduction in Actions runs vs. `strict`.

Trade-off accepted: no remote redundancy for PR events. The contract is
that delivery uses `deliver-to-main.sh`, which has already validated the
HEAD locally before push.

### off
Disables governance CI entirely.

- `governance.yml` early-exits on every event.
- `finalize-task-metadata.yml` early-exits on every PR merge.
- Authoritative gate is the local pre-commit hook installed via
  `scripts/install-pre-commit-hook.sh`.

Choose `off` only as a last resort when Actions cota must be reduced to
near-zero (e.g., billing emergency).

Trade-off: any developer using `git commit --no-verify` bypasses
governance entirely. Pair with manual review discipline.

## Configuration

The config file is optional. If present, it must contain a single token on
the first line:

```
strict
```

```
lite
```

```
off
```

Whitespace is trimmed. Anything else logs a warning and falls back to
`lite`.

### Switch to strict (PUBLIC repos)
```sh
echo strict > .governance/ci-mode.conf
git add .governance/ci-mode.conf
git commit -m "chore(ci): set governance CI mode to strict"
```

### Stay on default (lite)
Do nothing. Absence of the file is the default.

If a previous commit set a mode and you want to revert to the default:
```sh
git rm .governance/ci-mode.conf
git commit -m "chore(ci): revert governance CI mode to default (lite)"
```

### Switch to off (last resort)
```sh
echo off > .governance/ci-mode.conf
./scripts/install-pre-commit-hook.sh
git add .governance/ci-mode.conf
git commit -m "chore(ci): disable governance CI (mode=off); local hook is authoritative"
```

## Branch protection compatibility

All three modes always run at least the first step
("Resolve CI mode and decide early-exit"), so the workflow always reports
a status check. Early-exits use `exit 0`, which produces a successful
check — branch-protection required-check rules continue to be satisfied.

## Local pre-commit hook

`scripts/install-pre-commit-hook.sh` installs a `.git/hooks/pre-commit`
that runs `scripts/validate-all.sh` and propagates its exit code. It is
idempotent (running twice does not duplicate) and backs up any
pre-existing non-managed hook to `pre-commit.bak`.

The hook is required for mode `off`. It is also useful as additional
safety for `lite` and `strict` — even in those modes, the local hook
catches issues before push and avoids spending Actions minutes on
preventable failures.
