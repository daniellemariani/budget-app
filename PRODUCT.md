# Product - Budget App

**Version:** 0.1.0
**Status:** In Progress
**Owner:** Danielle Mariani
**Last Updated:** 2026-04-21

## Problem Statement
Managing a home budget through spreadsheets is fragile, error-prone, and inaccessible on mobile. There is no real-time visibility into spending by category, no tracking of credit card debt vs available funds, and no easy way to monitor progress toward financial goals. Every month starts from scratch with no accumulated insight from previous periods.

## Target Users

### Primary (Phase 1)
- Manages household finances for a family
- Tracks expenses across multiple accounts (checking, savings, credit cards, cash)
- Currently uses spreadsheets — functional but fragile
- Comfortable with smartphones, not technical
- Needs seamless mobile entry during daily errands

### Secondary (Phase 4+)
- Couples or families managing shared household finances
- Users who prefer privacy — no bank connection required
- Individuals who want a structured category-based budgeting
- General public seeking a simple budget tool

## Core User Workflow

1. Onboarding:
   - User enters initial account balances (e.g., Savings, Checking, Credit Card, Cash)

2. At the start of the month:
   - User enters expected income
   - User allocates planned spending by category
   - User defines optional goals (e.g., Hawaii trip)

3. During the month:
   - User logs expenses (quick entry)
   - User reviews "left to spend" per category

4. Mid-month adjustments:
   - User adjusts planned spending if needed

5. End of the month:
   - User reviews planned vs actual
   - User carries insights into next month
   - User reviews goals progress

## Goals

### Product Goals
- Replace spreadsheet with a faster, more intuitive mobile experience
- Seamless expense tracking and monthly planning
- Real-time visibility into budget vs. actual spending per category
- Full offline support — works without internet at all times

### Project Goals (Builder)
- Reactivate Android/Kotlin expertise with Jetpack Compose
- Learn Spec-Driven Development with AI-assisted tooling
- Build production-worthy architecture from Phase 1
- Optionally launch publicly if the product proves solid

## Non-Goals
- No bank integration
- No investment tracking
- No exportable reports or CSV export (Phase 1)
- No tax-related reporting
- No multi-currency support
- No financial advice or recommendations
- No Multi-user collaboration (Phase 4+ — see Workspace model in SPEC.md)

## Success Criteria

### Phase 1
- User uses the app daily instead of the spreadsheet
- User can add an expense in under 10 seconds
- Monthly budget setup takes under 5 minutes
- User can answer "am I over budget on groceries?" in under 3 taps
- Zero data loss incidents

### Public Launch (if pursued)
- 100 active users within 3 months of launch
- 4+ star rating on Google Play Store
- Day-30 retention above 40%

## Core Principles
- Speed over features: a fast simple experience beats a slow rich one
- Offline first: the app must work without internet at all times
- Real user validated: every feature must solve a confirmed real need
- Privacy by default: financial data never leaves the user's control

## Assumptions
- Primary user is on Android (iOS not required for Phase 1)
- User can use the app anonymously — no account creation or login required in Phase 1
- Single-user household budget in Phase 1 (multi-user collaboration introduced in Phase 4+)
- User manually enters all transactions (no import/export or integration with third-party solutions)
- English language support sufficient for target market

## Competitive Context
This app focuses on fast manual entry, offline-first operation, and simple monthly planning without requiring bank integrations or subscriptions.

## Open Questions
- Should the public version be free, freemium, or paid? or include Google Ads?
- Is receipt scanning a must-have for public launch?
- What geographic markets to target first beyond the US?
- Should the app support Spanish in addition to English for initial launch?
- Should Analytics be supported for public release?