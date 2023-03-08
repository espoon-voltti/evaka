// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import { differenceInDays } from 'date-fns'
import React, { createContext, useContext, useEffect, useMemo } from 'react'

import { Loading, Result } from 'lib-common/api'
import { MobileUser } from 'lib-common/api-types/employee-auth'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'
import { mockNow } from 'lib-common/utils/helpers'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'

import { renderResult } from '../async-rendering'
import { client } from '../client'
import { ServiceWorkerContext } from '../common/service-worker'

import { getAuthStatus, upsertPushSubscription } from './api'

export interface UserState {
  apiVersion: string | undefined
  loggedIn: boolean
  user: Result<MobileUser | null>
  refreshAuthStatus: () => void
}

export const UserContext = createContext<UserState>({
  apiVersion: undefined,
  loggedIn: false,
  user: Loading.of(),
  refreshAuthStatus: () => null
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children
}: {
  children: React.ReactNode
}) {
  const [authStatus, refreshAuthStatus] = useApiState(getAuthStatus, [])

  useEffect(
    () => idleTracker(client, refreshAuthStatus, { thresholdInMinutes: 20 }),
    [refreshAuthStatus]
  )

  const { registration } = useContext(ServiceWorkerContext)

  const pushNotifications = useMemo(() => {
    const user = authStatus.map(({ user }) => user).getOrElse(undefined)
    if (!user || !user.pushApplicationServerKey) return undefined
    if (!registration || !('pushManager' in registration)) return undefined
    return new PushNotifications(user.id, registration.pushManager, {
      userVisibleOnly: true,
      applicationServerKey: user.pushApplicationServerKey
    })
  }, [authStatus, registration])

  useEffect(() => {
    if (pushNotifications) {
      pushNotifications.enable().catch((err) => Sentry.captureException(err))
    }
  }, [pushNotifications])

  const value = useMemo(
    () => ({
      apiVersion: authStatus.map((a) => a.apiVersion).getOrElse(undefined),
      loggedIn: authStatus.map((a) => a.loggedIn).getOrElse(false),
      user: authStatus.map((a) => a.user ?? null),
      refreshAuthStatus
    }),
    [authStatus, refreshAuthStatus]
  )
  return (
    <UserContext.Provider value={value}>
      {renderResult(authStatus, () => (
        <>{children}</>
      ))}
    </UserContext.Provider>
  )
})

class PushNotifications {
  constructor(
    private device: UUID,
    private pushManager: PushManager,
    private options: PushSubscriptionOptionsInit
  ) {}

  async enable() {
    const sub = await this.refreshSubscription()
    const authSecret = sub?.getKey('auth')
    const ecdhKey = sub?.getKey('p256dh')
    if (sub && authSecret && ecdhKey) {
      await upsertPushSubscription(this.device, {
        endpoint: sub.endpoint,
        expires: sub.expirationTime
          ? HelsinkiDateTime.fromSystemTzDate(new Date(sub.expirationTime))
          : null,
        authSecret: Array.from(new Uint8Array(authSecret)),
        ecdhKey: Array.from(new Uint8Array(ecdhKey))
      })
    }
  }

  private async refreshSubscription(): Promise<PushSubscription | undefined> {
    const state = await this.pushManager.permissionState(this.options)
    if (state !== 'granted' && state !== 'prompt') {
      return undefined
    }
    const sub = await this.pushManager.getSubscription()
    if (sub) {
      const now = mockNow() ?? new Date()
      const expiringSoon = sub.expirationTime
        ? differenceInDays(now, sub.expirationTime) < 7
        : false
      if (!expiringSoon) {
        return sub
      }
    }
    return await this.pushManager.subscribe(this.options)
  }
}
