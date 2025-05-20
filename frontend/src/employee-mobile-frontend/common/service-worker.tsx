// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// For __IS_VITE__
/// <reference types="lib-common/env" />

import * as Sentry from '@sentry/browser'
import { differenceInDays } from 'date-fns'
import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { mockNow } from 'lib-common/utils/helpers'

import { UserContext } from '../auth/state'
import { upsertPushSubscription } from '../generated/api-clients/webpush'

interface ServiceWorkerState {
  registration: ServiceWorkerRegistration | undefined
  pushNotifications: PushNotifications | undefined
}

export const ServiceWorkerContext = createContext<ServiceWorkerState>({
  registration: undefined,
  pushNotifications: undefined
})

export const ServiceWorkerContextProvider = React.memo(
  function ServiceWorkerContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const user = useContext(UserContext).user.getOrElse(undefined)

    const [registration, setRegistration] =
      useState<ServiceWorkerRegistration>()

    const pushManager = useMemo(
      () =>
        registration && 'pushManager' in registration
          ? registration.pushManager
          : undefined,
      [registration]
    )

    const pushNotifications = useMemo(() => {
      if (!pushManager) return undefined
      return new PushNotifications(
        pushManager,
        user?.pushApplicationServerKey
          ? {
              userVisibleOnly: true,
              applicationServerKey: user.pushApplicationServerKey
            }
          : undefined
      )
    }, [user?.pushApplicationServerKey, pushManager])

    useEffect(() => {
      registerServiceWorker()
        .then(setRegistration)
        .catch((err) => {
          Sentry.captureException(err)
        })
    }, [])

    useEffect(() => {
      if (pushNotifications) {
        pushNotifications
          .refreshSubscription()
          .catch((err) => Sentry.captureException(err))
      } else if (pushManager) {
        unsubscribe(pushManager).catch((err) => Sentry.captureException(err))
      }
    }, [pushNotifications, pushManager])

    const value = { registration, pushNotifications }

    return (
      <ServiceWorkerContext.Provider value={value}>
        {children}
      </ServiceWorkerContext.Provider>
    )
  }
)

const registerServiceWorker = async () => {
  if ('serviceWorker' in navigator) {
    if (__IS_VITE__) {
      // Vite tranforms the referenced file
      const swUrl = new URL('../service-worker.js', import.meta.url)
      await navigator.serviceWorker.register(swUrl.href)
    } else {
      await navigator.serviceWorker.register(
        '/employee/mobile/service-worker.js'
      )
    }
    return await navigator.serviceWorker.ready
  } else {
    return undefined
  }
}

export class PushNotifications {
  constructor(
    private pushManager: PushManager,
    private options: PushSubscriptionOptionsInit | undefined
  ) {}

  get available(): boolean {
    return !!this.options
  }

  get permissionState(): Promise<PermissionState | undefined> {
    return this.options
      ? this.pushManager.permissionState(this.options)
      : Promise.resolve(undefined)
  }

  async enable(): Promise<boolean> {
    if (!(await this.requestPermission())) return false
    return await this.refreshSubscription()
  }

  async refreshSubscription(): Promise<boolean> {
    const sub = await this.getSubscription()
    const authSecret = sub?.getKey('auth')
    const ecdhKey = sub?.getKey('p256dh')
    if (sub && authSecret && ecdhKey) {
      await upsertPushSubscription({
        body: {
          endpoint: sub.endpoint,
          expires: sub.expirationTime
            ? HelsinkiDateTime.fromSystemTzDate(new Date(sub.expirationTime))
            : null,
          authSecret: Array.from(new Uint8Array(authSecret)),
          ecdhKey: Array.from(new Uint8Array(ecdhKey))
        }
      })
      return true
    }
    return false
  }

  async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      return false
    }
    switch (Notification.permission) {
      case 'granted':
        return true
      case 'denied':
        return false
      default:
        break
    }
    // support both legacy callback-based API and modern Promise API
    const result = await new Promise<NotificationPermission>(
      (resolve, reject) =>
        Notification.requestPermission(resolve)?.then(resolve, reject)
    )
    return result === 'granted'
  }

  private async getSubscription(): Promise<PushSubscription | undefined> {
    if (!this.options) {
      return undefined
    }
    const state = await this.pushManager.permissionState(this.options)
    if (state !== 'granted') {
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
      await sub.unsubscribe()
    }
    return await this.pushManager.subscribe(this.options)
  }
}

async function unsubscribe(pushManager: PushManager): Promise<void> {
  const sub = await pushManager.getSubscription()
  await sub?.unsubscribe()
}
