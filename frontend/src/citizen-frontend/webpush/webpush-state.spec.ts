// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import * as api from './webpush-api'
import { useWebPushState } from './webpush-state'

vi.mock('./webpush-api')

function mockBrowser(options: {
  hasServiceWorker: boolean
  hasPushManager: boolean
  permission: 'default' | 'granted' | 'denied'
  existingSubscription: {
    endpoint: string
    keys: { p256dh: string; auth: string }
  } | null
}) {
  const subscription = options.existingSubscription
    ? {
        endpoint: options.existingSubscription.endpoint,
        getKey: (name: 'p256dh' | 'auth') =>
          new TextEncoder().encode(options.existingSubscription!.keys[name])
            .buffer,
        unsubscribe: vi.fn().mockResolvedValue(true)
      }
    : null

  const pushManager = {
    getSubscription: vi.fn().mockResolvedValue(subscription),
    subscribe: vi.fn().mockResolvedValue({
      endpoint: 'https://fcm.example/new',
      getKey: (name: 'p256dh' | 'auth') =>
        new TextEncoder().encode(
          name === 'p256dh' ? 'p256dh-bytes' : 'auth-bytes'
        ).buffer,
      unsubscribe: vi.fn().mockResolvedValue(true)
    })
  }

  const registration = options.hasPushManager ? { pushManager } : {}

  if (options.hasServiceWorker) {
    Object.defineProperty(navigator, 'serviceWorker', {
      value: {
        ready: Promise.resolve(registration),
        getRegistration: vi.fn().mockResolvedValue(registration)
      },
      configurable: true,
      writable: true
    })
  } else {
    Object.defineProperty(navigator, 'serviceWorker', {
      value: undefined,
      configurable: true,
      writable: true
    })
  }

  if (options.hasPushManager) {
    // @ts-expect-error — jsdom doesn't define PushManager globally
    globalThis.PushManager = class {}
  } else {
    // @ts-expect-error — jsdom doesn't define PushManager globally
    delete globalThis.PushManager
  }

  Object.defineProperty(globalThis, 'Notification', {
    value: {
      permission: options.permission,
      requestPermission: vi.fn().mockResolvedValue(options.permission)
    },
    configurable: true,
    writable: true
  })
}

describe('useWebPushState', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('returns unsupported when ServiceWorker is missing', async () => {
    mockBrowser({
      hasServiceWorker: false,
      hasPushManager: false,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unsupported'))
  })

  it('returns unsupported when server returns null VAPID key (503)', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue(null)

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unsupported'))
  })

  it('returns unregistered when supported and no subscription yet', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unregistered'))
  })

  it('returns denied when permission is denied', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'denied',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('denied'))
  })

  it('returns subscribed when permission granted and existing subscription present', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'granted',
      existingSubscription: {
        endpoint: 'https://fcm.example/existing',
        keys: { p256dh: 'k', auth: 'a' }
      }
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('subscribed'))
  })

  it('subscribe() POSTs the new subscription with the chosen categories', async () => {
    mockBrowser({
      hasServiceWorker: true,
      hasPushManager: true,
      permission: 'default',
      existingSubscription: null
    })
    vi.mocked(api.getVapidKey).mockResolvedValue('vapid-public-key-base64')
    vi.mocked(api.putSubscription).mockResolvedValue({ sentTest: true })
    ;(globalThis.Notification as unknown as { permission: string }).permission =
      'default'
    // eslint-disable-next-line @typescript-eslint/unbound-method
    vi.mocked(globalThis.Notification.requestPermission).mockResolvedValue(
      'granted'
    )

    const { result } = renderHook(() => useWebPushState())
    await waitFor(() => expect(result.current.status).toBe('unregistered'))

    await act(async () => {
      await result.current.subscribe(new Set(['URGENT_MESSAGE', 'MESSAGE']))
    })

    expect(api.putSubscription).toHaveBeenCalledWith(
      expect.objectContaining({
        endpoint: 'https://fcm.example/new',
        enabledCategories: ['URGENT_MESSAGE', 'MESSAGE']
      })
    )
  })
})
