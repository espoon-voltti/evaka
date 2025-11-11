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

### Priority 1: Critical Path Tests (Run Now)
```bash
# Decisions tests - validates DecisionResponse.tsx
npm run e2e-test -- src/e2e-test/specs/0_citizen/citizen-decisions.spec.ts

# Income statement tests - validates IncomeStatementEditor.tsx
npm run e2e-test -- src/e2e-test/specs/0_citizen/citizen-income-statement.spec.ts

# Messaging tests - validates all 3 MessagesPage.tsx files
npm run e2e-test -- src/e2e-test/specs/7_messaging/messaging.spec.ts
npm run e2e-test -- src/e2e-test/specs/6_mobile/messages.spec.ts
```

### Priority 2: Extended Coverage (Run After Priority 1 Passes)
```bash
# Additional decision tests
npm run e2e-test -- src/e2e-test/specs/0_citizen/citizen-finance-decisions.spec.ts

# Additional income tests
npm run e2e-test -- src/e2e-test/specs/0_citizen/citizen-child-income-statement.spec.ts

# Staff messaging
npm run e2e-test -- src/e2e-test/specs/7_messaging/messaging-by-staff.spec.ts
```

## Estimated Test Time
- Priority 1: ~8-12 minutes total
- Priority 2: ~5-8 minutes total
- **Total: ~15-20 minutes**

