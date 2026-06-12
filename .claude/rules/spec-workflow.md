# Spec Workflow Rules

## Before Writing Any Code

1. Identify the current task ID from `specs/features/[feature]/tasks.md`
2. Read `specs/features/[feature]/requirements.md` for behavior and business rules
3. Read `specs/features/[feature]/design.md` for architecture decisions and ViewModel shape
4. Confirm the task's `Creates:` file list — only create those files, nothing extra

## Task Execution Rules

- Implement exactly one task at a time (one TSK-XX-NN)
- Create only the files listed in the task's `Creates:` field
- Do not create files for future tasks — even if they seem obviously needed
- After completing a task, report: files created, files modified, anything that needs review

## Business Rules

- Business rules are identified by IDs (e.g. BR-DI-01, BR-BU-03)
- Never rename or renumber a BR ID once assigned
- Feature specs reference global rules by ID only — never copy the rule text
- If a rule conflicts with a spec, stop and ask — do not resolve silently

## When Something Is Ambiguous

Stop and ask. Do not assume. Specifically:
- If the spec doesn't say how to handle an edge case → ask
- If a task's `Details:` guidance conflicts with the `design.md` → ask
- If a required file path looks wrong → ask before creating it

## What Counts as "Done"

A task is done when:
- All files in `Creates:` exist at the correct paths
- The implementation matches the spec (requirements + design)
- No hardcoded strings (all in `strings.xml`)
- No SharedPreferences direct access (all through `PreferencesDataSource`)
- The feature compiles and the task's acceptance criteria pass

## Feature Spec File Reference

```
specs/features/[feature]/
├── requirements.md   — behavior, business rules, ACs, error handling
├── design.md         — ViewModel state, use cases, DI wiring, design tokens
└── tasks.md          — ordered task list with file paths and dependencies
```

## Current Implementation Order (Phase 1)

Onboarding → Accounts → Categories → Merchants → Transactions → Transfers → Budgets → Goals → Dashboard
