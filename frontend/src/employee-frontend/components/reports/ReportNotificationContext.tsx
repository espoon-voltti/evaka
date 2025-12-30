// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useContext } from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { featureFlags } from 'lib-customizations/employee'

import { childDocumentDecisionsReportNotificationCountQuery } from '../../queries'
import { UserContext } from '../../state/user'

export interface ReportNotificationState {
  childDocumentDecisionNotificationCount: Result<number>
}

const defaultState: ReportNotificationState = {
  childDocumentDecisionNotificationCount: Loading.of()
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

    return (
      <ReportNotificationContext.Provider
        value={{
          childDocumentDecisionNotificationCount
        }}
      >
        {children}
      </ReportNotificationContext.Provider>
    )
  }
)
