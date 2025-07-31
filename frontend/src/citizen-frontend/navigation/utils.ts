// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'
import sumBy from 'lodash/sumBy'
import type { KeyboardEvent } from 'react'
import { useCallback, useMemo } from 'react'

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useQuery } from 'lib-common/query'

import type { User } from '../auth/state'
import { useUser } from '../auth/state'
import { unreadChildDocumentsCountQuery } from '../child-documents/queries'
import { childrenQuery } from '../children/queries'
import { unreadPedagogicalDocumentsCountQuery } from '../children/sections/pedagogical-documents/queries'
import { assistanceDecisionUnreadCountsQuery } from '../decisions/assistance-decision-page/queries'
import { assistanceNeedPreschoolDecisionUnreadCountsQuery } from '../decisions/assistance-decision-page/queries-preschool'
import { applicationNotificationsQuery } from '../decisions/queries'

const empty = {}

export function useUnreadChildNotifications() {
  const loggedIn = useUser() !== undefined
  const { data: unreadPedagogicalDocumentsCount = empty } = useQuery(
    unreadPedagogicalDocumentsCountQuery(),
    { enabled: loggedIn }
  )
  const { data: unreadChildDocumentsCount = empty } = useQuery(
    unreadChildDocumentsCountQuery(),
    { enabled: loggedIn }
  )

  const unreadChildNotifications = useMemo(() => {
    const counts: Record<ChildId, number> = {}
    const addCounts = (countRecord: Record<ChildId, number>) =>
      Object.entries(countRecord).forEach(([id, count]) => {
        counts[id as ChildId] = (counts[id as ChildId] ?? 0) + count
      })

    addCounts(unreadPedagogicalDocumentsCount)
    addCounts(unreadChildDocumentsCount)

    return counts
  }, [unreadPedagogicalDocumentsCount, unreadChildDocumentsCount])

  const totalUnreadChildNotifications = useMemo(
    () => sum(Object.values(unreadChildNotifications)),
    [unreadChildNotifications]
  )

  return { unreadChildNotifications, totalUnreadChildNotifications }
}

export function useChildrenWithOwnPage() {
  const { data } = useQuery(childrenQuery())
  return useMemo(() => {
    if (!data) return []
    return data.filter((child) => child.upcomingPlacementType !== null)
  }, [data])
}

export function useUnreadDecisions() {
  const loggedIn = useUser() !== undefined
  const { data: unreadDaycareAssistanceDecisionCounts = [] } = useQuery(
    assistanceDecisionUnreadCountsQuery(),
    { enabled: loggedIn }
  )
  const { data: unreadPreschoolAssistanceDecisionCounts = [] } = useQuery(
    assistanceNeedPreschoolDecisionUnreadCountsQuery(),
    { enabled: loggedIn }
  )
  const { data: decisionWaitingConfirmationCount = 0 } = useQuery(
    applicationNotificationsQuery(),
    { enabled: loggedIn }
  )

  return (
    decisionWaitingConfirmationCount +
    sumBy(unreadDaycareAssistanceDecisionCounts, ({ count }) => count) +
    sumBy(unreadPreschoolAssistanceDecisionCounts, ({ count }) => count)
  )
}

export const isPersonalDetailsIncomplete = (user: User) => !user.email

export const useOnEscape = (action: () => void) => {
  return useCallback(
    (event: KeyboardEvent<HTMLElement>) => {
      if (event.key === 'Escape') {
        action()
      }
    },
    [action]
  )
}
