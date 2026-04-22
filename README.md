# Budget App

> A personal budget management app built offline-first for Android, with a web dashboard and backend sync planned for later phases.

## Status
Phase 1 in progress — Android offline app

| Phase | Description | Status |
|---|---|---|
| 1 | Android app, offline, no backend | In Progress |
| 2 | FastAPI backend + Android sync | Not Started |
| 3 | React web dashboard | Not Started |
| 4 | Cross-platform mobile (Flutter/KMP) | Not Started |

## Overview
Budget App is a personal finance management tool that allows users to track expenses, manage monthly budgets per category, and visualize spending patterns. Built offline-first for Android, with a web dashboard and backend sync in later phases.

Link to PRODUCT.md for full product vision.
Link to SPEC.md for full feature spec.

## Tech Stack

### Android (Phase 1)
- Kotlin + Jetpack Compose
- Hilt (dependency injection)
- Room (local database)
- Coroutines + Flow

### Backend (Phase 2)
- Python + FastAPI
- PostgreSQL + SQLModel

### Web (Phase 3)
- React + TypeScript

## Project Structure
budget-app/
├── android/        # Jetpack Compose Android app
├── backend/        # FastAPI backend (Phase 2)
├── web/            # React web app (Phase 3)
├── specs/          # Feature and technical specs
└── .cursor/rules/  # Cursor AI rules

## Documentation
| Document | Purpose |
|---|---|
| PRODUCT.md | Vision, users, success criteria |
| SPEC.md | Global definitions, glossary, feature index |
| ARCHITECTURE.md | Technical decisions and rationale |
| ROADMAP.md | Phase timeline and progress |
| specs/ | Data model, API contract, sync strategy |
| specs/features/ | Per-feature specs and tasks |

## Getting Started

### Prerequisites
- Android Studio Panda or later
- JDK 17+
- Android SDK 35

### Running the Android app
cd android
# Open in Android Studio and run on emulator or device

## Architecture
Brief summary of key decisions with link to ARCHITECTURE.md.
- Offline-first: Room is source of truth in Phase 1
- MVVM + Repository pattern
- Feature-based package structure

## Development

### Spec-Driven Development
This project follows SDD:
1. Read root context files: SPEC.md, PRODUCT.md, ARCHITECTURE.md, ROADMAP.md
1. Read specs/features/[feature]/spec.md
2. Check specs/data-model.md
3. Follow tasks in specs/features/[feature]/tasks.md

## License
Private — not licensed for public use yet.