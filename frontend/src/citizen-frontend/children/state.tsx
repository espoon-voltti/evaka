// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import { UnreadAssistanceNeedDecisionItem } from 'lib-common/generated/api-types/assistanceneed'
import { CitizenChildConsent } from 'lib-common/generated/api-types/children'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getAssistanceNeedDecisionUnreadCounts, getChildConsents } from './api'

interface ChildrenContext {
  unreadAssistanceNeedDecisionCounts: UnreadAssistanceNeedDecisionItem[]
  refreshUnreadAssistanceNeedDecisionCounts: () => void
  childConsents: Result<Record<UUID, CitizenChildConsent[]>>
  refreshChildConsents: () => void
}

const defaultValue: ChildrenContext = {
  unreadAssistanceNeedDecisionCounts: [],
  refreshUnreadAssistanceNeedDecisionCounts: () => undefined,
  childConsents: Loading.of(),
  refreshChildConsents: () => undefined
}

export const ChildrenContext = createContext(defaultValue)

export const ChildrenContextProvider = React.memo(
  function ChildrenContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const user = useUser()

    const [
      unreadAssistanceNeedDecisionCounts,
      refreshUnreadAssistanceNeedDecisionCounts
    ] = useApiState(
      () =>
        !user
          ? Promise.resolve(Success.of([]))
          : getAssistanceNeedDecisionUnreadCounts(),
      [user]
    )

    const [childConsents, refreshChildConsents] = useApiState(
      () => (!user ? Promise.resolve(Success.of({})) : getChildConsents()),
      [user]
    )

    return (
      <ChildrenContext.Provider
        value={{
          unreadAssistanceNeedDecisionCounts:
            unreadAssistanceNeedDecisionCounts.getOrElse([]),
          refreshUnreadAssistanceNeedDecisionCounts,
          childConsents,
          refreshChildConsents
        }}
      >
        {children}
      </ChildrenContext.Provider>
    )
  }
)
