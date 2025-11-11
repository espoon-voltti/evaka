<!--
SPDX-FileCopyrightText: 2017-2025 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# ESLint Rules Removal - Session Progress Summary

## Completed Work

### Rule 1: react-hooks/refs ✅ FULLY COMPLETED
- **Status:** Rule removed, all 5 files fixed, e2e tests passed
- **Files fixed:** 5
- **E2e tests:** citizen-calendar (6/6 passed)
- **Quality:** Production-ready

### Rule 2: react-hooks/set-state-in-effect ⚠️ PARTIALLY COMPLETED  
- **Status:** Rule removed, 7/35 files fixed
- **Files fixed:** 7 (20% complete)
- **E2e tests:** Not yet run

#### Files Fixed This Session (Rule 2)
1. ✅ DatePicker.tsx - getDerivedStateFromProps pattern
2. ✅ DateRangePicker.tsx - getDerivedStateFromProps pattern  
3. ✅ DailyServiceTimeNotifications.tsx - getDerivedStateFromProps pattern
4. ✅ calendar/hooks.ts (2 functions) - getDerivedStateFromProps pattern
5. ✅ useReplyRecipients.ts - getDerivedStateFromProps pattern
6. ✅ UnitList.tsx - getDerivedStateFromProps pattern

## Approach & Patterns Used

### Pattern 1: getDerivedStateFromProps (Most Common)
**Used for:** Syncing state with props

**Before:**
```typescript
const [value, setValue] = useState(initialValue)
useEffect(() => {
  setValue(propValue)
}, [propValue])
```

**After:**
```typescript
const [state, setState] = useState({
  value: initialValue,
  prevProp: initialValue
})
if (state.prevProp !== propValue) {
  setState({
    value: propValue,
    prevProp: propValue
  })
}
```

### Pattern 2: Async Data Loading (Legitimate but needs proper handling)
For cases like:
- Loading data from API
- Debounced saves  
- Accumulating paginated results

**Proper fix:** Refactor to use proper async state management, not suppressions.

## Remaining Work

### Rule 2: set-state-in-effect (28 files remaining)

**Category A - Simple Sync (6 remaining):**
- citizen-frontend/messages/MessageEditor.tsx
- employee-frontend/components/messages/MessageEditor.tsx  
- employee-frontend/components/person-shared/PersonDetails.tsx
- employee-frontend/components/reports/AssistanceNeedDecisionsReport.tsx
- employee-frontend/components/reports/MissingServiceNeed.tsx
- employee-frontend/components/reports/StartingPlacements.tsx

**Category B - Async Operations (5 files):**
- citizen-frontend/calendar/hooks.ts (useExtendedReservationsRange)
- employee-frontend/utils/use-autosave.ts
- employee-frontend/components/messages/useDraft.ts  
- lib-components/Notifications.tsx
- employee-mobile-frontend/child-attendance/AttendanceList.tsx

**Category C - Complex Forms (17 files):**
All form/editor components in employee-frontend

### Estimated Effort  
- Category A: ~2-3 hours (straightforward pattern)
- Category B: ~3-4 hours (requires careful async refactoring)
- Category C: ~6-8 hours (complex components, need thorough testing)
- **Total:** ~11-15 hours remaining for Rule 2

### Rule 3 & 4
- Not started
- Estimated ~8-10 hours each

## Next Steps

1. **Immediate:** Continue fixing Category A files (simple pattern application)
2. **Then:** Tackle Category B with proper async patterns
3. **Finally:** Work through Category C form components
4. **After each category:** Run relevant e2e tests
5. **After Rule 2 complete:** Run full regression suite
6. **Then:** Move to Rules 3 and 4

## Key Learnings

1. **getDerivedStateFromProps pattern is powerful** - Solves most prop-to-state sync cases
2. **Track previous prop values in state** - Enables proper comparison without refs
3. **Combine multiple state pieces** - Reduces re-renders and keeps related data together
4. **E2e tests are critical** - Calendar tests passed, validating our approach
5. **Systematic approach works** - Categorizing files by pattern type speeds up fixes

## Recommendations for Continuation

1. **Fix in batches:** Complete 3-5 files, then run relevant e2e tests
2. **Document patterns:** As new patterns emerge, add them to this document
3. **Test incrementally:** Don't wait until all files are fixed
4. **Prioritize high-traffic components:** Messages, forms, calendar features
5. **Consider component refactoring:** Some complex components may benefit from architectural improvements

---

*Last updated: 2025-11-11*
