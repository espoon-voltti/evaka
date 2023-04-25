// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'
import { useMemo } from 'react'

import type { Child } from 'lib-common/generated/api-types/children'
import { useQuery } from 'lib-common/query'
import type { UUID } from 'lib-common/types'

import { useUser } from '../auth/state'
import { childrenQuery } from '../children/queries'
import { childConsentNotificationsQuery } from '../children/sections/consents/queries'
import { unreadPedagogicalDocumentsCountQuery } from '../children/sections/pedagogical-documents/queries'
import { unreadVasuDocumentsCountQuery } from '../children/sections/vasu-and-leops/queries'

const empty = {}

export function useUnreadChildNotifications() {
  const loggedIn = useUser() !== undefined
  const { data: childConsentNotifications = empty } = useQuery(
    childConsentNotificationsQuery,
    { enabled: loggedIn }
  )
  const { data: unreadPedagogicalDocumentsCount = empty } = useQuery(
    unreadPedagogicalDocumentsCountQuery,
    { enabled: loggedIn }
  )
  const { data: unreadVasuDocumentsCount = empty } = useQuery(
    unreadVasuDocumentsCountQuery,
    { enabled: loggedIn }
  )

  const unreadChildNotifications = useMemo(() => {
    const counts: Record<UUID, number> = {}
    const addCounts = (countRecord: Record<UUID, number>) =>
      Object.entries(countRecord).forEach(([id, count]) => {
        counts[id] = (counts[id] ?? 0) + count
      })

    addCounts(unreadPedagogicalDocumentsCount)
    addCounts(unreadVasuDocumentsCount)
    addCounts(childConsentNotifications)

    return counts
  }, [
    childConsentNotifications,
    unreadPedagogicalDocumentsCount,
    unreadVasuDocumentsCount
  ])

  const totalUnreadChildNotifications = useMemo(
    () => sum(Object.values(unreadChildNotifications)),
    [unreadChildNotifications]
  )

  return { unreadChildNotifications, totalUnreadChildNotifications }
}

export function useChildrenWithOwnPage(): Child[] {
  const { data } = useQuery(childrenQuery)
  return useMemo(() => {
    if (!data) return []
    return data.filter(
      (child) =>
        child.upcomingPlacementType !== null ||
        child.hasPedagogicalDocuments ||
        child.hasCurriculums
    )
  }, [data])
}
