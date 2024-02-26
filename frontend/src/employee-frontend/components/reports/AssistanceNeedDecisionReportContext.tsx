// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useContext } from 'react'

import { UserContext } from 'employee-frontend/state/user'
import { Loading, Result, Success, wrapResult } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getAssistanceNeedDecisionsReportUnreadCount } from '../../generated/api-clients/reports'

const getAssistanceNeedDecisionsReportUnreadCountResult = wrapResult(
  getAssistanceNeedDecisionsReportUnreadCount
)

export interface AssistanceNeedDecisionReportState {
  assistanceNeedDecisionCounts: Result<number>
  refreshAssistanceNeedDecisionCounts: () => void
}

const defaultState: AssistanceNeedDecisionReportState = {
  assistanceNeedDecisionCounts: Loading.of(),
  refreshAssistanceNeedDecisionCounts: () => undefined
}

export const AssistanceNeedDecisionReportContext =
  createContext<AssistanceNeedDecisionReportState>(defaultState)

export const AssistanceNeedDecisionReportContextProvider = React.memo(
  function AssistanceNeedDecisionReportContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const { user } = useContext(UserContext)

    const [assistanceNeedDecisionCounts, refreshAssistanceNeedDecisionCounts] =
      useApiState(
        () =>
          user?.accessibleFeatures.assistanceNeedDecisionsReport
            ? getAssistanceNeedDecisionsReportUnreadCountResult()
            : Promise.resolve(Success.of(0)),
        [user]
      )

    return (
      <AssistanceNeedDecisionReportContext.Provider
        value={{
          assistanceNeedDecisionCounts,
          refreshAssistanceNeedDecisionCounts
        }}
      >
        {children}
      </AssistanceNeedDecisionReportContext.Provider>
    )
  }
)
