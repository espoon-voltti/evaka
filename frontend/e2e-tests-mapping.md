<!--
SPDX-FileCopyrightText: 2017-2025 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# E2E Tests Mapping for Fixed Components

## Session Files Fixed and Corresponding Tests

### 1. DecisionResponse.tsx (citizen-frontend/decisions/decision-response-page/)
**E2E Tests:**
- `src/e2e-test/specs/0_citizen/citizen-decisions.spec.ts` - Tests decision acceptance/rejection flows
- `src/e2e-test/specs/0_citizen/citizen-finance-decisions.spec.ts` - Finance decisions

### 2. IncomeStatementEditor.tsx (citizen-frontend/income-statements/)
**E2E Tests:**
- `src/e2e-test/specs/0_citizen/citizen-income-statement.spec.ts` - Main test for income statement editor
- `src/e2e-test/specs/0_citizen/citizen-child-income-statement.spec.ts` - Child income statements
- `src/e2e-test/specs/0_citizen/citizen-income.spec.ts` - Income flows

### 3. Messages Components (3 files)
- `citizen-frontend/messages/MessagesPage.tsx`
- `employee-frontend/components/messages/MessagesPage.tsx`
- `employee-mobile-frontend/messages/MessagesPage.tsx`

**E2E Tests:**
- `src/e2e-test/specs/7_messaging/messaging.spec.ts` - Main messaging tests (citizen + employee)
- `src/e2e-test/specs/7_messaging/messaging-by-staff.spec.ts` - Staff messaging
- `src/e2e-test/specs/6_mobile/messages.spec.ts` - Mobile messaging

## Test Execution Plan

### IMPORTANT: Use npm run e2e-ci
```bash
# ⚠️ Use e2e-ci NOT e2e-test (e2e-test runs full suite and ignores args!)
npm run e2e-ci -- <test-file>

# To run specific tests within a file:
npm run e2e-ci -- <test-file> -g "test name pattern"
```

### Priority 1: Critical Smoke Tests (Run Now)

#### 1. Decision Acceptance/Rejection (DecisionResponse.tsx)
```bash
# Run specific critical tests - decision acceptance flow with date validation
npm run e2e-ci -- src/e2e-test/specs/0_citizen/citizen-decisions.spec.ts -g "accept decision"
```
**Validates:** Date validation state management, decision response flows

#### 2. Income Statement Editor (IncomeStatementEditor.tsx)
```bash
# Run form validation and error scrolling tests
npm run e2e-ci -- src/e2e-test/specs/0_citizen/citizen-income-statement.spec.ts -g "can submit|validation"
```
**Validates:** Form validation, scroll-to-error functionality (our DOM side-effect fix)

#### 3. Citizen Messaging (citizen-frontend/messages/MessagesPage.tsx)
```bash
# Run editor visibility and message sending tests
npm run e2e-ci -- src/e2e-test/specs/7_messaging/messaging.spec.ts -g "Citizen can send a message|Citizen can view messages"
```
**Validates:** Editor visibility derived from URL params, message list/editor switching

#### 4. Employee Messaging (employee-frontend/messages/MessagesPage.tsx)
```bash
# Run employee message editor and draft tests
npm run e2e-ci -- src/e2e-test/specs/7_messaging/messaging.spec.ts -g "Staff can send a message|draft"
```
**Validates:** Editor visibility synced with draft selection, message sending

#### 5. Mobile Messaging (employee-mobile-frontend/messages/MessagesPage.tsx)
```bash
# Run mobile message navigation tests
npm run e2e-ci -- src/e2e-test/specs/6_mobile/messages.spec.ts -g "can send|can navigate"
```
**Validates:** UI state reset on unit/group change, message navigation

### Priority 2: Extended Coverage (Optional - Run if Priority 1 Passes)
```bash
# Full decision tests
npm run e2e-ci -- src/e2e-test/specs/0_citizen/citizen-decisions.spec.ts

# Full messaging tests  
npm run e2e-ci -- src/e2e-test/specs/7_messaging/messaging.spec.ts

# Mobile messaging full suite
npm run e2e-ci -- src/e2e-test/specs/6_mobile/messages.spec.ts
```

## Estimated Test Time
- Priority 1 (Smoke tests): ~3-5 minutes total (5 targeted test runs)
- Priority 2 (Full suites): ~8-12 minutes total
- **Recommended: Run Priority 1 first, expand if issues found**

