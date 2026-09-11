// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'
import { differenceInDays } from 'date-fns'
import { useCallback, useEffect, useSyncExternalStore } from 'react'

import type { CitizenPushSubscriptionId } from 'lib-common/generated/api-types/shared'
import type { WebPushSubscription } from 'lib-common/generated/api-types/webpush'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { featureFlags } from 'lib-customizations/citizen'

import { useUser } from '../auth/state'

import { useIsRunningInstalled } from './installed'
import {
  addPushSubscriptionMutation,
  pushSettingsQuery,
  pushSubscriptionCheckQuery
} from './queries'

export type PushAvailability =
  | { kind: 'unavailable' }
  | { kind: 'blocked' }
  | { kind: 'subscribable' }
  | { kind: 'subscribed' }

const pushSupported = () =>
  'serviceWorker' in navigator &&
  'PushManager' in window &&
  'Notification' in window

interface BrowserPushState {
  permission: NotificationPermission
  subscription: PushSubscription | null
}

let browserState: BrowserPushState | undefined = undefined
let reading = false
const listeners = new Set<() => void>()

const subscribeToBrowserState = (onChange: () => void) => {
  listeners.add(onChange)
  return () => {
    listeners.delete(onChange)
  }
}

async function readBrowserState(): Promise<void> {
  const registration = await navigator.serviceWorker.ready
  browserState = {
    permission: Notification.permission,
    subscription: await registration.pushManager.getSubscription()
  }
  listeners.forEach((listener) => listener())
}

function useBrowserPushState(): BrowserPushState | undefined {
  useEffect(() => {
    if (
      browserState === undefined &&
      !reading &&
      featureFlags.citizenPwa &&
      pushSupported()
    ) {
      reading = true
      readBrowserState()
        .catch((err) => Sentry.captureException(err))
        .finally(() => {
          reading = false
        })
    }
  }, [])
  return useSyncExternalStore(subscribeToBrowserState, () => browserState)
}

/** Null when web push is not configured in this environment, undefined if requests pending */
function useApplicationServerKey(): string | null | undefined {
  const user = useUser()
  const settings = useQueryResult(
    user && featureFlags.citizenPwa && pushSupported()
      ? pushSettingsQuery()
      : constantQuery(null)
  )
  return settings
    .map((s) => s?.applicationServerKey ?? null)
    .getOrElse(undefined)
}

/**
 * The id of this browser's subscription in the citizen's push device list,
 * null when the browser has no subscription of theirs, undefined if requests
 * are pending.
 *
 * A subscription belongs to a browser and outlives the session, so the subscription
 * in this browres may be another user's. The backend destroys a subscription and
 * responds with null if the subscription belongs to another user.
 */
export function useThisPushDevice():
  | CitizenPushSubscriptionId
  | null
  | undefined {
  const user = useUser()
  const browser = useBrowserPushState()
  const endpoint = browser?.subscription?.endpoint
  const check = useQueryResult(
    user && endpoint !== undefined
      ? pushSubscriptionCheckQuery({ body: { endpoint } })
      : constantQuery(null)
  )
  const deviceId = check.map((r) => r?.deviceId ?? null).getOrElse(undefined)

  useEffect(() => {
    if (user && endpoint !== undefined && deviceId === null) {
      unsubscribeLocally().catch((err) => Sentry.captureException(err))
    }
  }, [user, endpoint, deviceId])

  if (!user || browser === undefined) return undefined
  return deviceId
}

export function usePushAvailability(): PushAvailability {
  const runningInstalled = useIsRunningInstalled()
  const applicationServerKey = useApplicationServerKey()
  const browser = useBrowserPushState()
  const device = useThisPushDevice()

  if (!featureFlags.citizenPwa || !runningInstalled || !pushSupported())
    return { kind: 'unavailable' }
  if (
    applicationServerKey === undefined ||
    browser === undefined ||
    device === undefined
  )
    return { kind: 'unavailable' }
  if (applicationServerKey === null) return { kind: 'unavailable' }
  if (browser.permission === 'denied') return { kind: 'blocked' }
  return device !== null ? { kind: 'subscribed' } : { kind: 'subscribable' }
}

/**
 * Returns a callback that asks for the notification permission and subscribes this browser.
 *
 * The callback **must be called from a user gesture**.
 */
export function useSubscribeToPush(): () => Promise<void> {
  const applicationServerKey = useApplicationServerKey()
  const installed = useIsRunningInstalled()
  const { mutateAsync: addSubscription } = useMutationResult(
    addPushSubscriptionMutation
  )
  return useCallback(async () => {
    if (!applicationServerKey) return
    try {
      if (await requestPermission()) {
        const subscription = await subscribeInBrowser(applicationServerKey)
        if (subscription) {
          await addSubscription({ body: { subscription, installed } })
        }
      }
      await readBrowserState()
    } catch (err) {
      Sentry.captureException(err)
    }
  }, [addSubscription, applicationServerKey, installed])
}

export async function unsubscribeLocally(): Promise<void> {
  const registration = await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.getSubscription()
  await subscription?.unsubscribe()
  await readBrowserState()
}

async function requestPermission(): Promise<boolean> {
  switch (Notification.permission) {
    case 'granted':
      return true
    case 'denied':
      return false
    default:
      break
  }
  // support both the legacy callback-based API and the modern promise API
  const result = await new Promise<NotificationPermission>((resolve, reject) =>
    Notification.requestPermission(resolve)?.then(resolve, reject)
  )
  return result === 'granted'
}

async function subscribeInBrowser(
  applicationServerKey: string
): Promise<WebPushSubscription | undefined> {
  const registration = await navigator.serviceWorker.ready
  const options: PushSubscriptionOptionsInit = {
    userVisibleOnly: true,
    applicationServerKey
  }
  let existing = await registration.pushManager.getSubscription()
  if (existing && expiringSoon(existing)) {
    await existing.unsubscribe()
    existing = null
  }
  const subscription =
    existing ?? (await registration.pushManager.subscribe(options))

  const authSecret = subscription.getKey('auth')
  const ecdhKey = subscription.getKey('p256dh')
  if (!authSecret || !ecdhKey) return undefined
  return {
    endpoint: subscription.endpoint,
    expires: subscription.expirationTime
      ? HelsinkiDateTime.fromSystemTzDate(new Date(subscription.expirationTime))
      : null,
    authSecret: Array.from(new Uint8Array(authSecret)),
    ecdhKey: Array.from(new Uint8Array(ecdhKey))
  }
}

const expiringSoon = (subscription: PushSubscription): boolean =>
  subscription.expirationTime
    ? differenceInDays(
        subscription.expirationTime,
        HelsinkiDateTime.now().toSystemTzDate()
      ) < 7
    : false
