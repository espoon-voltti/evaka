// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UseQueryResult } from '@tanstack/react-query'
import sum from 'lodash/sum'
import { useEffect } from 'react'

import { useQuery } from 'lib-common/query'

import { useUser } from '../auth/state'
import { unreadChildDocumentsCountQuery } from '../child-documents/queries'
import { unreadPedagogicalDocumentsCountQuery } from '../children/sections/pedagogical-documents/queries'
import { applicationNotificationsQuery } from '../decisions/queries'
import { unreadMessagesCountQuery } from '../messages/queries'

import type { AppBadgeSource } from './appBadgeCount'
import { appBadgeCount } from './appBadgeCount'

function setAppBadge(count: number) {
  if (!('setAppBadge' in navigator)) return
  navigator.setAppBadge(count).catch(() => undefined)
}

function badgeQuery<T>(
  query: UseQueryResult<T, unknown>,
  count: (data: T) => number
): { source: AppBadgeSource; dataUpdatedAt: number } {
  const source =
    query.data !== undefined
      ? count(query.data)
      : query.isError
        ? 'failed'
        : 'loading'
  return { source, dataUpdatedAt: query.dataUpdatedAt }
}

const sumValues = (counts: Partial<Record<string, number>>) =>
  sum(Object.values(counts))

// Must match getCitizenAppBadgeCount in the backend
export function useAppBadge(): void {
  const loggedIn = useUser() !== undefined
  const queries = [
    badgeQuery(
      useQuery(unreadMessagesCountQuery(), { enabled: loggedIn }),
      (n) => n
    ),
    badgeQuery(
      useQuery(applicationNotificationsQuery(), { enabled: loggedIn }),
      (n) => n
    ),
    badgeQuery(
      useQuery(unreadPedagogicalDocumentsCountQuery(), { enabled: loggedIn }),
      sumValues
    ),
    badgeQuery(
      useQuery(unreadChildDocumentsCountQuery(), { enabled: loggedIn }),
      sumValues
    )
  ]
  const count = appBadgeCount(
    loggedIn,
    queries.map((q) => q.source)
  )
  const lastFetchedAt = Math.max(...queries.map((q) => q.dataUpdatedAt))

  // A push may have set a newer count while the app was hidden, so only freshly
  // fetched data overwrites the badge
  useEffect(() => {
    if (count === undefined || document.visibilityState !== 'visible') return
    setAppBadge(count)
  }, [count, lastFetchedAt])
}
