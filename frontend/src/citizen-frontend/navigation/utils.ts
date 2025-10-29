// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInfo } from '@fortawesome/free-solid-svg-icons'
import sum from 'lodash/sum'
import sumBy from 'lodash/sumBy'
import type { KeyboardEvent } from 'react'
import {
  useCallback,
  useContext,
  useEffect,
  useEffectEvent,
  useMemo
} from 'react'

import type { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { useQuery, useQueryResult } from 'lib-common/query'
import useLocalStorage from 'lib-common/utils/useLocalStorage'
import { NotificationsContext } from 'lib-components/Notifications'
import { colors } from 'lib-customizations/common'

import type { User } from '../auth/state'
import { useUser } from '../auth/state'
import { unreadChildDocumentsCountQuery } from '../child-documents/queries'
import { childrenQuery } from '../children/queries'
import { unreadPedagogicalDocumentsCountQuery } from '../children/sections/pedagogical-documents/queries'
import { assistanceDecisionUnreadCountsQuery } from '../decisions/assistance-decision-page/queries'
import { assistanceNeedPreschoolDecisionUnreadCountsQuery } from '../decisions/assistance-decision-page/queries-preschool'
import { applicationNotificationsQuery } from '../decisions/queries'
import { useTranslation } from '../localization'

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

type DismissedNotifications = Record<string, { dismissedAt: number }>
type ChildWithUpcomingPlacement = ChildAndPermittedActions & {
  upcomingPlacementStartDate: LocalDate
  upcomingPlacementUnit: {
    id: string
    name: string
  }
}

// Type predicate function that acts as both filter and type guard
function hasUpcomingPlacement(
  child: ChildAndPermittedActions
): child is ChildWithUpcomingPlacement {
  return (
    child.upcomingPlacementType !== null &&
    child.upcomingPlacementStartDate !== null &&
    child.upcomingPlacementUnit !== null &&
    !child.upcomingPlacementIsCalendarOpen
  )
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isDismissedEntry(value: unknown): value is { dismissedAt: number } {
  return (
    isRecord(value) &&
    'dismissedAt' in value &&
    typeof value.dismissedAt === 'number'
  )
}

function validateDismissedNotifications(
  parsed: unknown
): parsed is DismissedNotifications {
  if (!isRecord(parsed)) return false

  for (const [key, value] of Object.entries(parsed)) {
    if (typeof key !== 'string' || !isDismissedEntry(value)) {
      return false
    }
  }
  return true
}

const parseDismissedNotifications = (str: string): DismissedNotifications => {
  let parsed: unknown
  try {
    parsed = JSON.parse(str)
  } catch {
    parsed = {}
  }
  if (validateDismissedNotifications(parsed)) {
    return parsed
  }
  return {}
}

const reNotifyAgainAfterDismissal = 7 * 24 * 60 * 60 * 1000 // 7 days
const notificationPrefix = 'child-not-started-toast'

export const useChildrenStartingNotification = () => {
  const loggedIn = useUser() !== undefined
  const i18n = useTranslation()
  const [dismissedNotificationsJson, setDismissedNotificationsJson] =
    useLocalStorage(
      'children-starting-notifications',
      '{}',
      (value) => typeof value === 'string'
    )
  const dismissedNotifications = useMemo(
    () => parseDismissedNotifications(dismissedNotificationsJson),
    [dismissedNotificationsJson]
  )

  const childrenResult = useQueryResult(childrenQuery(), { enabled: loggedIn })

  const { addNotification, removeNotifications } =
    useContext(NotificationsContext)

  const dismissNotification = useCallback(
    (childId: string) => {
      setDismissedNotificationsJson((prev) => {
        let dismissedNotificationsUpdate: DismissedNotifications = {}
        try {
          dismissedNotificationsUpdate = prev
            ? parseDismissedNotifications(prev)
            : {}
        } catch {
          dismissedNotificationsUpdate = {}
        }
        dismissedNotificationsUpdate[childId] = { dismissedAt: Date.now() }
        return JSON.stringify(dismissedNotificationsUpdate)
      })
    },
    [setDismissedNotificationsJson]
  )

  const onNotificationDataLoaded = useEffectEvent(
    (
      upcomingChildren: ChildWithUpcomingPlacement[],
      _dismissedNotifications: DismissedNotifications
    ) => {
      removeNotifications(notificationPrefix)
      const upcomingChildIds: string[] = []
      upcomingChildren.forEach((child, index) => {
        upcomingChildIds.push(child.id.toString())

        const dismissedAt = _dismissedNotifications[child.id]?.dismissedAt
        if (
          dismissedAt &&
          Date.now() - dismissedAt < reNotifyAgainAfterDismissal
        ) {
          return
        }

        addNotification(
          {
            icon: faInfo,
            iconColor: colors.main.m2,
            children: i18n.calendar.infoToast.childStartsInfo(
              formatPersonName(child, 'FirstFirst'),
              child.upcomingPlacementStartDate.format(),
              child.upcomingPlacementUnit.name
            ),
            onClose: () => dismissNotification(child.id),
            dataQa: `${notificationPrefix}-${index}`
          },
          `${notificationPrefix}-${index}`
        )
      })
      const storedNotificationChildIds = Object.keys(_dismissedNotifications)

      // Check if there are any stale entries
      const staleIds = storedNotificationChildIds.filter(
        (id) => !upcomingChildIds.includes(id)
      )

      if (staleIds.length > 0) {
        setDismissedNotificationsJson((prev) => {
          const current = parseDismissedNotifications(prev)
          const dismissedNotificationsUpdate: DismissedNotifications = {}

          // Keep only entries for actual upcoming children
          upcomingChildIds.forEach((childId) => {
            if (current[childId]) {
              dismissedNotificationsUpdate[childId] = current[childId]
            }
          })
          return JSON.stringify(dismissedNotificationsUpdate)
        })
      }
    }
  )

  useEffect(() => {
    if (childrenResult.isSuccess) {
      const upcomingChildren = childrenResult.value.filter(hasUpcomingPlacement)
      onNotificationDataLoaded(upcomingChildren, dismissedNotifications)
    }
  }, [childrenResult, dismissedNotifications])
}
