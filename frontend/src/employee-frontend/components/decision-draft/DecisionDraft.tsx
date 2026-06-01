// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { UserContext } from '../../state/user'

import DecisionDraftLegacy from './DecisionDraftLegacy'
import DecisionDraftRedesign from './DecisionDraftRedesign'

export default React.memo(function DecisionDraft() {
  const { user } = useContext(UserContext)
  return user?.accessibleFeatures.decisionReasoningsEnabled === true ? (
    <DecisionDraftRedesign />
  ) : (
    <DecisionDraftLegacy />
  )
})
