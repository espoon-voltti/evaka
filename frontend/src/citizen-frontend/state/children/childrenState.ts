import { skipToken } from '@reduxjs/toolkit/query'
import sum from 'lodash/sum'
import { useMemo } from 'react'

import { UUID } from 'lib-common/types'

import { useUser } from '../../auth/state'

import {
  useChildConsentNotificationsQuery,
  useChildrenQueryResult,
  useUnreadPedagogicalDocumentsCountQuery,
  useUnreadVasuDocumentsCountQuery
} from './childrenApi'

export function useChildUnreadNotifications() {
  const user = useUser()

  const { data: unreadPedagogicalDocumentsCount } =
    useUnreadPedagogicalDocumentsCountQuery(!user ? skipToken : undefined)
  const { data: unreadVasuDocumentsCount } = useUnreadVasuDocumentsCountQuery(
    !user ? skipToken : undefined
  )
  const { data: childConsentNotifications } = useChildConsentNotificationsQuery(
    !user ? skipToken : undefined
  )

  const unreadChildNotifications = useMemo(() => {
    const counts: Record<UUID, number> = {}
    const addCounts = (countRecord: Record<UUID, number>) =>
      Object.entries(countRecord).forEach(([id, count]) => {
        counts[id] = (counts[id] ?? 0) + count
      })

    addCounts(unreadPedagogicalDocumentsCount ?? {})
    addCounts(unreadVasuDocumentsCount ?? {})
    addCounts(childConsentNotifications ?? {})

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

export function useChildren() {
  const user = useUser()
  return useChildrenQueryResult(!user ? skipToken : undefined)
}

export function useChildrenWithOwnPage() {
  const childrenResponse = useChildren()
  return useMemo(
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
}
