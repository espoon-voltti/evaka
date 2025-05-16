// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useContext } from 'react'

import type { Result } from 'lib-common/api'
import { Loading, Success, wrapResult } from 'lib-common/api'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { useApiState } from 'lib-common/utils/useRestApi'
import { featureFlags } from 'lib-customizations/employee'

import { getAssistanceNeedDecisionsReportUnreadCount } from '../../generated/api-clients/reports'
import { childDocumentDecisionsReportNotificationCountQuery } from '../../queries'
import { UserContext } from '../../state/user'

const getAssistanceNeedDecisionsReportUnreadCountResult = wrapResult(
  getAssistanceNeedDecisionsReportUnreadCount
)

export interface ReportNotificationState {
  childDocumentDecisionNotificationCount: Result<number>
  assistanceNeedDecisionCounts: Result<number>
  refreshAssistanceNeedDecisionCounts: () => void
}

const defaultState: ReportNotificationState = {
  childDocumentDecisionNotificationCount: Loading.of(),
  assistanceNeedDecisionCounts: Loading.of(),
  refreshAssistanceNeedDecisionCounts: () => undefined
}

export const ReportNotificationContext =
  createContext<ReportNotificationState>(defaultState)

export const ReportNotificationContextProvider = React.memo(
  function ReportNotificationContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const { user } = useContext(UserContext)

    const childDocumentDecisionNotificationCount = useQueryResult(
      user?.permittedGlobalActions?.includes(
        'READ_CHILD_DOCUMENT_DECISIONS_REPORT'
      ) && featureFlags.decisionChildDocumentTypes
        ? childDocumentDecisionsReportNotificationCountQuery()
        : constantQuery(0),
      {
        staleTime: 30 * 1000, // refetch at most every 30 seconds
        refetchInterval: 10 * 60 * 1000 // refetch at least every 10 minutes
      }
    )

    const [assistanceNeedDecisionCounts, refreshAssistanceNeedDecisionCounts] =
      useApiState(
        () =>
          user?.accessibleFeatures.assistanceNeedDecisionsReport
            ? getAssistanceNeedDecisionsReportUnreadCountResult()
            : Promise.resolve(Success.of(0)),
        [user]
      )

    return (
      <ReportNotificationContext.Provider
        value={{
          childDocumentDecisionNotificationCount,
          assistanceNeedDecisionCounts,
          refreshAssistanceNeedDecisionCounts
        }}
      >
        {children}
      </ReportNotificationContext.Provider>
    )
  }
)
