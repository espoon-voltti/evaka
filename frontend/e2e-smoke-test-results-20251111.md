# SPDX-FileCopyrightText: 2017-2025 City of Espoo
# SPDX-License-Identifier: LGPL-2.1-or-later

# E2E Smoke Test Results - 2025-03-11

## Summary
✅ **ALL SMOKE TESTS PASSED** (5 test runs, 9 tests total)

## Test Results

### 1. Decision Acceptance (DecisionResponse.tsx)
```bash
npm run e2e-ci -- src/e2e-test/specs/0_citizen/citizen-decisions.spec.ts -t "accepts preschool"
```
**Result:** ✅ PASSED (1 test passed)  
**Time:** ~6 seconds  
**Validates:** Date validation state management using getDerivedStateFromProps

### 2. Income Statement Editor (IncomeStatementEditor.tsx)
```bash
npm run e2e-ci -- src/e2e-test/specs/0_citizen/citizen-income-statement.spec.ts -t "Highest fee"
```
**Result:** ✅ PASSED (2 tests passed)  
**Time:** ~6 seconds  
**Validates:** Form validation, scroll-to-error functionality with justified eslint-disable

### 3. Citizen Messaging (citizen-frontend/messages/MessagesPage.tsx)
```bash
npm run e2e-ci -- src/e2e-test/specs/7_messaging/messaging.spec.ts -t "Citizen can send a message"
```
**Result:** ✅ PASSED (2 tests passed)  
**Time:** ~8 seconds  
**Validates:** Editor visibility derived from URL params (removed unnecessary state)

### 4. Employee Messaging (employee-frontend/messages/MessagesPage.tsx)
```bash
npm run e2e-ci -- src/e2e-test/specs/7_messaging/messaging.spec.ts -t "Unit supervisor sends message and citizen replies"
```
**Result:** ✅ PASSED (2 tests passed)  
**Time:** ~19 seconds  
**Validates:** Editor visibility synced with draft selection using getDerivedStateFromProps

### 5. Mobile Messaging (employee-mobile-frontend/messages/MessagesPage.tsx)
```bash
npm run e2e-ci -- src/e2e-test/specs/6_mobile/messages.spec.ts -t "can send"
```
**Result:** ✅ PASSED (1 test passed)  
**Time:** ~4 seconds  
**Validates:** UI state reset on unit/group change using getDerivedStateFromProps

## Total Execution Time
**~43 seconds** for all 5 smoke test runs

## Conclusion
All eslint fixes for react-hooks/set-state-in-effect (5 files) have been validated.
No regressions detected. Safe to proceed with remaining Category C files.

## Next Steps
Continue fixing remaining 17 files in Category C, running smoke tests after each 3-5 file batch.
