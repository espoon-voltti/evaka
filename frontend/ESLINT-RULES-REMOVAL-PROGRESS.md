<!--
SPDX-FileCopyrightText: 2017-2025 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# ESLint Rules Removal Progress

## Rule 1: react-hooks/refs ✅ COMPLETED

**Status:** All violations fixed

**Files fixed:**
1. `useStableCallback.ts` - Moved ref update to useEffect
2. `DatePicker.tsx` - Used state-based getDerivedStateFromProps pattern
3. `DateRangePicker.tsx` - Used state-based getDerivedStateFromProps pattern  
4. `CalendarListView.tsx` - Extracted ref value, added eslint-disable for false positive
5. `Combobox.tsx` - Added eslint-disable for downshift library requirement

**Approach:**
- Moved ref updates from render to useEffect where possible
- For date pickers, refactored to use state instead of refs for tracking previous values
- Added targeted eslint-disable comments where library requirements conflict with the rule

---

## Rule 2: react-hooks/set-state-in-effect ⚠️  80% COMPLETED

**Status:** 37 files fixed, 9 remaining

**Files fixed:**
1-23. (Previous sessions - see PROGRESS-SUMMARY.md)
24. `ApplicationPage.tsx` - getDerivedStateFromProps for init + validation
25. `PlacementDesktop.tsx` - getDerivedStateFromProps + justified eslint-disable
26. `ChildDocumentEditView.tsx` - justified eslint-disable for side effects
27. `AssistanceNeedVoucherCoefficientForm.tsx` - useMemo for validation
28. `AssistanceActionForm.tsx` - useMemo + error separation

**Remaining files (9):** See BATCH-2-COMPLETE.md for list

**Common patterns identified:**
1. **Prop-to-state sync** - Components using useEffect to update state when props change
   - Fix: Use getDerivedStateFromProps pattern (setState during render with prev value tracking)
   
2. **Validation side effects** - Forms running validation in useEffect
   - Fix: Move validation to event handlers or use render-time state updates
   
3. **Autosave/drafts** - Legitimate async operations in effects
   - Fix: May need targeted eslint-disable comments or refactoring to separate concerns

**Recommendation:** Given the scope (35 files), consider:
- Fixing high-traffic components first (calendar, forms, messages)
- Adding targeted eslint-disable with explanations for legitimate cases
- Planning a gradual migration strategy

---

## Rule 3: react-hooks/preserve-manual-memoization - NOT STARTED

## Rule 4: react-hooks/static-components - NOT STARTED

