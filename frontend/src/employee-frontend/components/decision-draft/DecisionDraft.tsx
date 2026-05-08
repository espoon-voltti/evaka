// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { featureFlags } from 'lib-customizations/employee'

import DecisionDraftLegacy from './DecisionDraftLegacy'
import DecisionDraftRedesign from './DecisionDraftRedesign'

export default React.memo(function DecisionDraft() {
  return featureFlags.decisionDraftRedesign === true ? (
    <DecisionDraftRedesign />
  ) : (
    <DecisionDraftLegacy />
  )
})
