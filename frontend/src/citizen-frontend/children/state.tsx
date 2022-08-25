// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'
import sumBy from 'lodash/sumBy'
import React, { createContext, useMemo } from 'react'

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
  refreshChildConsentNotifications: () => void
  unreadPedagogicalDocumentsCount: Record<UUID, number> | undefined
  unreadVasuDocumentsCount: Record<UUID, number> | undefined
  refreshUnreadPedagogicalDocumentsCount: () => void
  refreshUnreadVasuDocumentsCount: () => void
  unreadChildNotifications: number
}

const defaultValue: ChildrenContext = {
  unreadAssistanceNeedDecisionCounts: [],
  refreshUnreadAssistanceNeedDecisionCounts: () => undefined,
  childConsents: Loading.of(),
  refreshChildConsents: () => undefined,
  refreshChildConsentNotifications: () => undefined,
  unreadPedagogicalDocumentsCount: undefined,
  unreadVasuDocumentsCount: undefined,
  refreshUnreadPedagogicalDocumentsCount: () => undefined,
  refreshUnreadVasuDocumentsCount: () => undefined,
  unreadChildNotifications: 0
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

    const unreadChildNotifications = useMemo(() => {
      const unreadAssistanceNeedDecisionCount = sumBy(
        unreadAssistanceNeedDecisionCounts.getOrElse([]),
        ({ count }) => count
      )
      const unreadChildDocumentsCount =
        sum(Object.values(unreadPedagogicalDocumentsCount.getOrElse({}))) +
        sum(Object.values(unreadVasuDocumentsCount.getOrElse({})))
      return (
        unreadAssistanceNeedDecisionCount +
        childConsentNotifications.getOrElse(0) +
        unreadChildDocumentsCount
      )
    }, [
      childConsentNotifications,
      unreadAssistanceNeedDecisionCounts,
      unreadPedagogicalDocumentsCount,
      unreadVasuDocumentsCount
    ])

    return (
      <ChildrenContext.Provider
        value={{
          unreadAssistanceNeedDecisionCounts:
            unreadAssistanceNeedDecisionCounts.getOrElse([]),
          refreshUnreadAssistanceNeedDecisionCounts,
          childConsents,
          refreshChildConsents,
          refreshChildConsentNotifications,
          unreadPedagogicalDocumentsCount:
            unreadPedagogicalDocumentsCount.getOrElse(undefined),
          unreadVasuDocumentsCount:
            unreadVasuDocumentsCount.getOrElse(undefined),
          refreshUnreadPedagogicalDocumentsCount,
          refreshUnreadVasuDocumentsCount,
          unreadChildNotifications
        }}
      >
        {children}
      </ChildrenContext.Provider>
    )
  }
)
