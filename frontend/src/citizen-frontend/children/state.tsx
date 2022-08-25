// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'
import React, { createContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import { UnreadAssistanceNeedDecisionItem } from 'lib-common/generated/api-types/assistanceneed'
import {
  Child,
  CitizenChildConsent
} from 'lib-common/generated/api-types/children'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getChildren } from './api'
import { getAssistanceNeedDecisionUnreadCounts } from './sections/assistance-need-decision/api'
import {
  getChildConsentNotifications,
  getChildConsents
} from './sections/consents/api'
import { getUnreadPedagogicalDocumentsCount } from './sections/pedagogical-documents/api'
import { getUnreadVasuDocumentsCount } from './sections/vasu-and-leops/api'

interface ChildrenContext {
  children: Result<Child[]>
  unreadAssistanceNeedDecisionCounts: UnreadAssistanceNeedDecisionItem[]
  refreshUnreadAssistanceNeedDecisionCounts: () => void
  childConsents: Result<Record<UUID, CitizenChildConsent[]>>
  refreshChildConsents: () => void
  refreshChildConsentNotifications: () => void
  unreadPedagogicalDocumentsCount: Record<UUID, number> | undefined
  unreadVasuDocumentsCount: Record<UUID, number> | undefined
  refreshUnreadPedagogicalDocumentsCount: () => void
  refreshUnreadVasuDocumentsCount: () => void
  unreadChildNotifications: Record<UUID, number>
  totalUnreadChildNotifications: number
}

const defaultValue: ChildrenContext = {
  children: Loading.of(),
  unreadAssistanceNeedDecisionCounts: [],
  refreshUnreadAssistanceNeedDecisionCounts: () => undefined,
  childConsents: Loading.of(),
  refreshChildConsents: () => undefined,
  refreshChildConsentNotifications: () => undefined,
  unreadPedagogicalDocumentsCount: undefined,
  unreadVasuDocumentsCount: undefined,
  refreshUnreadPedagogicalDocumentsCount: () => undefined,
  refreshUnreadVasuDocumentsCount: () => undefined,
  unreadChildNotifications: {},
  totalUnreadChildNotifications: 0
}

export const ChildrenContext = createContext(defaultValue)

export const ChildrenContextProvider = React.memo(
  function ChildrenContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const user = useUser()

    const [childrenResponse] = useApiState(
      () => (!user ? Promise.resolve(Success.of([])) : getChildren()),
      [user]
    )

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
            ? Promise.resolve(Success.of({}))
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
      const counts: Record<UUID, number> = {}
      const addCounts = (countRecord: Record<UUID, number>) =>
        Object.entries(countRecord).forEach(([id, count]) => {
          counts[id] = (counts[id] ?? 0) + count
        })

      unreadAssistanceNeedDecisionCounts
        .getOrElse<UnreadAssistanceNeedDecisionItem[]>([])
        .forEach(({ childId, count }) => {
          counts[childId] = (counts[childId] ?? 0) + count
        })
      addCounts(unreadPedagogicalDocumentsCount.getOrElse({}))
      addCounts(unreadVasuDocumentsCount.getOrElse({}))
      addCounts(childConsentNotifications.getOrElse({}))

      return counts
    }, [
      childConsentNotifications,
      unreadAssistanceNeedDecisionCounts,
      unreadPedagogicalDocumentsCount,
      unreadVasuDocumentsCount
    ])

    const totalUnreadChildNotifications = sum(
      Object.values(unreadChildNotifications)
    )

    return (
      <ChildrenContext.Provider
        value={{
          children: childrenResponse,
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
          unreadChildNotifications,
          totalUnreadChildNotifications
        }}
      >
        {children}
      </ChildrenContext.Provider>
    )
  }
)
