// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { featureFlags } from 'lib-customizations/employee'

import ApplicationPageLegacy from './ApplicationPageLegacy'
import ApplicationPageShared from './ApplicationPageShared'

// Temporary: the shared citizen/employee editor is being validated in staging
// before it replaces the legacy employee-only one everywhere. The two pages keep
// their own state models — the legacy one edits ApplicationDetails in place,
// the shared one edits ApplicationFormData — so they cannot be merged behind a
// conditional inside a single component.
export default React.memo(function ApplicationPage() {
  return featureFlags.sharedApplicationEditor ? (
    <ApplicationPageShared />
  ) : (
    <ApplicationPageLegacy />
  )
})
