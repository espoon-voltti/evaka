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

import { getAssistanceNeedDecisionUnreadCounts } from './sections/assistance-need-decision/api'
import {
  getChildConsentNotifications,
  getChildConsents
} from './sections/consents/api'
import { getUnreadPedagogicalDocumentsCount } from './sections/pedagogical-documents/api'
import { getUnreadVasuDocumentsCount } from './sections/vasu-and-leops/api'

interface ChildrenContext {
  unreadAssistanceNeedDecisionCounts: UnreadAssistanceNeedDecisionItem[]
  refreshUnreadAssistanceNeedDecisionCounts: () => void
  childConsents: Result<Record<UUID, CitizenChildConsent[]>>
  refreshChildConsents: () => void
  childConsentNotifications: number
  refreshChildConsentNotifications: () => void
  unreadPedagogicalDocumentsCount: Record<UUID, number> | undefined
  unreadVasuDocumentsCount: Record<UUID, number> | undefined
  refreshUnreadPedagogicalDocumentsCount: () => void
  refreshUnreadVasuDocumentsCount: () => void
}

const defaultValue: ChildrenContext = {
  unreadAssistanceNeedDecisionCounts: [],
  refreshUnreadAssistanceNeedDecisionCounts: () => undefined,
  childConsents: Loading.of(),
  refreshChildConsents: () => undefined,
  childConsentNotifications: 0,
  refreshChildConsentNotifications: () => undefined,
  unreadPedagogicalDocumentsCount: undefined,
  unreadVasuDocumentsCount: undefined,
  refreshUnreadPedagogicalDocumentsCount: () => undefined,
  refreshUnreadVasuDocumentsCount: () => undefined
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
      () =>
        user?.authLevel === 'STRONG'
          ? getChildConsents()
          : Promise.resolve(Success.of({})),
      [user]
    )

    const [childConsentNotifications, refreshChildConsentNotifications] =
      useApiState(
        () =>
          !user
            ? Promise.resolve(Success.of(0))
            : getChildConsentNotifications(),
        [user]
      )

    const [
      unreadPedagogicalDocumentsCount,
      refreshUnreadPedagogicalDocumentsCount
    ] = useApiState(
      () =>
        !user
          ? Promise.resolve(Success.of({}))
          : getUnreadPedagogicalDocumentsCount(),
      [user]
    )

    const [unreadVasuDocumentsCount, refreshUnreadVasuDocumentsCount] =
      useApiState(
        () =>
          !user
            ? Promise.resolve(Success.of({}))
            : getUnreadVasuDocumentsCount(),
        [user]
      )

    return (
      <ChildrenContext.Provider
        value={{
          unreadAssistanceNeedDecisionCounts:
            unreadAssistanceNeedDecisionCounts.getOrElse([]),
          refreshUnreadAssistanceNeedDecisionCounts,
          childConsents,
          refreshChildConsents,
          childConsentNotifications: childConsentNotifications.getOrElse(0),
          refreshChildConsentNotifications,
          unreadPedagogicalDocumentsCount:
            unreadPedagogicalDocumentsCount.getOrElse(undefined),
          unreadVasuDocumentsCount:
            unreadVasuDocumentsCount.getOrElse(undefined),
          refreshUnreadPedagogicalDocumentsCount,
          refreshUnreadVasuDocumentsCount
        }}
      >
        {children}
      </ChildrenContext.Provider>
    )
  }
)
