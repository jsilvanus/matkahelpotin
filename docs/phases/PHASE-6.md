# Phase 6 — Chromium Integration and Production Hardening

## Goal

Build the separate Chromium extension that consumes Matkahelpotin exports and assists the user with importing records into target systems.

## Extension

- TypeScript.
- Manifest V3.
- ZIP import.
- Manifest/schema/checksum validation.
- Import preview.
- Dataset selection.
- Generic browser workflow engine.
- Content scripts for page interaction.
- Service worker for extension orchestration.
- Explicit user-visible actions and verification points.
- Import progress and recoverable failure state.

## Target adapters

Keep target-specific logic isolated:

```text
targets/
├── tax-service/
└── visma-intu/
```

The Android app must not contain target-system DOM knowledge.

## Human-in-the-loop workflow

The extension should detect the expected target page, prepare the next value, perform an explicit browser action where appropriate, and allow the user to verify the result before proceeding. It should not behave as a blind submission bot.

## Hardening

- Export/import compatibility tests.
- Database migration tests.
- Malformed ZIP/JSON handling.
- Duplicate/import detection.
- Target-page detection failures.
- Extension permission minimization.
- Secure message validation between extension components.
- Accessibility and localisation review.
- Release signing/build automation.

## Exit criteria

A user can export a selected period from Android, import the ZIP into the extension, select a target, and work through a visible, recoverable import workflow with user verification at the appropriate points.
