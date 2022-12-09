// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createSelector } from '@reduxjs/toolkit'
import { skipToken } from '@reduxjs/toolkit/query'
import sum from 'lodash/sum'
import { defaultMemoize } from 'reselect'

import { Result } from 'lib-common/api'
import { Child } from 'lib-common/generated/api-types/children'
import { UUID } from 'lib-common/types'

import { useUser } from '../../auth/state'

import {
  useChildConsentNotificationsQuery,
  useChildrenQueryResult,
  useUnreadPedagogicalDocumentsCountQuery,
  useUnreadVasuDocumentsCountQuery
} from './childrenApi'

type UnreadCountsInput = {
  unreadPedagogicalDocumentsCount: Record<UUID, number> | undefined
  unreadVasuDocumentsCount: Record<UUID, number> | undefined
  childConsentNotifications: Record<UUID, number> | undefined
}

const unreadCountsSelector = createSelector(
  (input: UnreadCountsInput) => input.unreadPedagogicalDocumentsCount,
  (input: UnreadCountsInput) => input.unreadVasuDocumentsCount,
  (input: UnreadCountsInput) => input.childConsentNotifications,
  (
    unreadPedagogicalDocumentsCount,
    unreadVasuDocumentsCount,
    childConsentNotifications
  ) => {
    const counts: Record<UUID, number> = {}

    const addCounts = (countRecord: Record<UUID, number>) =>
      Object.entries(countRecord).forEach(([id, count]) => {
        counts[id] = (counts[id] ?? 0) + count
      })

    // Report counts as 0 until all data is available
    if (
      unreadPedagogicalDocumentsCount &&
      unreadVasuDocumentsCount &&
      childConsentNotifications
    ) {
      addCounts(unreadPedagogicalDocumentsCount)
      addCounts(unreadVasuDocumentsCount)
      addCounts(childConsentNotifications)
    }

    const total = sum(Object.values(counts))
    return { counts, total }
  }
)

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

  const { counts, total } = unreadCountsSelector({
    unreadPedagogicalDocumentsCount,
    unreadVasuDocumentsCount,
    childConsentNotifications
  })

  return {
    unreadChildNotifications: counts,
    totalUnreadChildNotifications: total
  }
}

const childrenWithOwnPageSelector = defaultMemoize(
  (childrenResponse: Result<Child[]>) =>
    childrenResponse.map((children) =>
      children.filter(
        (child) =>
          child.hasUpcomingPlacements ||
          child.hasPedagogicalDocuments ||
          child.hasCurriculums
      )
    )
)

export function useChildrenWithOwnPage() {
  return childrenWithOwnPageSelector(useChildrenQueryResult())
}
