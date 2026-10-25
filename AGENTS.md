# AGENTS.md

Spigot plugin: a staff toolkit (vanish, freeze, invsee, staff mode, inspect, notes, audit). Java, Spigot API only (no Paper APIs). NMS and reflection are allowed only inside per-version modules behind an interface in the main plugin; the plugin must run, with that feature disabled, when no module matches the server version.

## Code

- Minimum code that solves the task and stays maintainable. Delete before adding.
- No comments, Javadoc, or TODOs. Intention-revealing names; extract instead of explaining.
- No speculative abstractions: no single-implementation interfaces, no unrequested config. Exception: the interface in front of a per-version NMS module.
- No new dependencies without approval.
- Composition over inheritance, except where Spigot forces it.
- Immutable by default: `final`, records, unmodifiable collections.
- Guard clauses over nesting. No `null` for absence; `Optional` at API boundaries only.
- No static mutable state. Constructor injection.
- Never swallow exceptions; handle at the boundary that can act on them.

## Domain

- Every staff action is a service call that writes its own audit entry. Commands, GUIs and API calls hold no logic, so none can skip the audit.
- The audit log is append-only. Deletes are soft.
- Staff actions form a closed set: enum or sealed type, exhaustive `switch`.
- A staff member can never act on someone of a higher staff level, whatever their permission nodes say.
- Vanished players leak nothing: every broadcast, list, count, target and sound about them checks vanish state.
- State that must survive relog, respawn, world change or a crash (vanish, freeze, staff-mode snapshots) lives in storage. Write snapshots before the change they protect.
- `java.time` (`Instant`, `Duration`) in signatures, never raw millis.
- Identify players by `UUID`, never name.
- Parse and validate input at the command boundary; core logic takes typed values.
- Other plugins (Warrant, LuckPerms) are soft dependencies. Features fall back or hide when they are absent.
- User-facing messages come from one message source.

## Concurrency & Persistence

- No I/O on the main thread. Bukkit API calls return to the main thread via the scheduler.
- Async event handlers never touch world or entity state.
- Plain JDBC, `PreparedStatement` only, no ORM. Migrations are additive.

## Workflow

A human owns every change. The agent assists; it does not ship.

- Propose a short plan and wait for approval before editing.
- One concern per change. Stop and propose a split if it spans several or exceeds ~400 changed lines.
- Never commit, push, branch, open PRs, or file issues. Never write PR or issue text.
- Explain non-obvious decisions so the human can defend them in review.
- If a task needs changes outside the stated scope, flag it and ask first.
- No linked issue and not a trivial `fix/` or `docs/` change: ask for an issue first.

## Git

- Branches: `feature/<slug>`, `fix/<slug>`, `docs/<slug>`. Small, merged often.
- Squash merge into `main`, linear history.
- PRs link their issue (`Closes #N`) and fully follow `.github/pull_request_template.md`.
- Imperative commit subjects under 72 characters. No unrelated reformatting.

## Responses

- Lead with the code. No preamble or recap. Show only changed code.
- For features and fixes, end with a short verification list: key cases, edge cases, failure scenarios.
- Unit-test pure logic without a server. Do not mock what you can construct.
