// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { skipToken } from '@reduxjs/toolkit/query'
import sum from 'lodash/sum'
import React, { createContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import { UnreadAssistanceNeedDecisionItem } from 'lib-common/generated/api-types/assistanceneed'
import { Child } from 'lib-common/generated/api-types/children'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import {
  useChildConsentNotificationsQueryResult,
  useUnreadVasuDocumentsCountQueryResult
} from '../state/children/childrenApi'

import { getChildren, getAssistanceNeedDecisionUnreadCounts } from './api'
import { getUnreadPedagogicalDocumentsCount } from './sections/pedagogical-documents/api'

interface ChildrenContext {
  children: Result<Child[]>
  childrenWithOwnPage: Result<Child[]>
  unreadAssistanceNeedDecisionCounts: UnreadAssistanceNeedDecisionItem[]
  refreshUnreadAssistanceNeedDecisionCounts: () => void
  unreadPedagogicalDocumentsCount: Record<UUID, number> | undefined
  unreadVasuDocumentsCount: Record<UUID, number> | undefined
  refreshUnreadPedagogicalDocumentsCount: () => void
  unreadChildNotifications: Record<UUID, number>
  totalUnreadChildNotifications: number
}

const defaultValue: ChildrenContext = {
  children: Loading.of(),
  childrenWithOwnPage: Loading.of(),
  unreadAssistanceNeedDecisionCounts: [],
  refreshUnreadAssistanceNeedDecisionCounts: () => undefined,
  unreadPedagogicalDocumentsCount: undefined,
  unreadVasuDocumentsCount: undefined,
  refreshUnreadPedagogicalDocumentsCount: () => undefined,
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

    const childrenWithOwnPage = useMemo(
      () =>
        childrenResponse.map((children) =>
          children.filter(
            (child) =>
              child.hasUpcomingPlacements ||
              child.hasPedagogicalDocuments ||
              child.hasCurriculums
          )
        ),
      [childrenResponse]
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

    const unreadVasuDocumentsCount = useUnreadVasuDocumentsCountQueryResult(
      !user ? skipToken : undefined
    )
    const childConsentNotifications = useChildConsentNotificationsQueryResult(
      !user ? skipToken : undefined
    )

    const unreadChildNotifications = useMemo(() => {
      const counts: Record<UUID, number> = {}
      const addCounts = (countRecord: Record<UUID, number>) =>
        Object.entries(countRecord).forEach(([id, count]) => {
          counts[id] = (counts[id] ?? 0) + count
        })

      addCounts(unreadPedagogicalDocumentsCount.getOrElse({}))
      addCounts(unreadVasuDocumentsCount.getOrElse({}))
      addCounts(childConsentNotifications.getOrElse({}))

      return counts
    }, [
      childConsentNotifications,
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
          childrenWithOwnPage,
          unreadAssistanceNeedDecisionCounts:
            unreadAssistanceNeedDecisionCounts.getOrElse([]),
          refreshUnreadAssistanceNeedDecisionCounts,
          unreadPedagogicalDocumentsCount:
            unreadPedagogicalDocumentsCount.getOrElse(undefined),
          unreadVasuDocumentsCount:
            unreadVasuDocumentsCount.getOrElse(undefined),
          refreshUnreadPedagogicalDocumentsCount,
          unreadChildNotifications,
          totalUnreadChildNotifications
        }}
      >
        {children}
      </ChildrenContext.Provider>
    )
  }
)
