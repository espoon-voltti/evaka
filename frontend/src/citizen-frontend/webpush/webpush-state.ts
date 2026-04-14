// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'

import type { CitizenPushCategory } from './webpush-api'
import {
  deleteSubscription,
  getVapidKey,
  postTest,
  putSubscription
} from './webpush-api'

export type WebPushStatus =
  | 'loading'
  | 'unsupported'
  | 'unregistered'
  | 'denied'
  | 'subscribed'

export interface UseWebPushStateResult {
  status: WebPushStatus
  categories: Set<CitizenPushCategory>
  subscribe: (categories: Set<CitizenPushCategory>) => Promise<void>
  updateCategories: (categories: Set<CitizenPushCategory>) => Promise<void>
  unsubscribe: () => Promise<void>
  sendTest: () => Promise<void>
}

const ALL_CATEGORIES: CitizenPushCategory[] = [
  'URGENT_MESSAGE',
  'MESSAGE',
  'BULLETIN'
]

const SESSION_KEY = 'evaka-webpush-categories'

function loadCategories(endpoint: string): Set<CitizenPushCategory> {
  if (typeof sessionStorage === 'undefined') return new Set(ALL_CATEGORIES)
  try {
    const raw = sessionStorage.getItem(`${SESSION_KEY}:${endpoint}`)
    if (!raw) return new Set(ALL_CATEGORIES)
    const parsed = JSON.parse(raw) as CitizenPushCategory[]
    return new Set(parsed)
  } catch {
    return new Set(ALL_CATEGORIES)
  }
}

function persistCategories(
  endpoint: string,
  categories: Set<CitizenPushCategory>
): void {
  if (typeof sessionStorage === 'undefined') return
  try {
    sessionStorage.setItem(
      `${SESSION_KEY}:${endpoint}`,
      JSON.stringify(Array.from(categories))
    )
  } catch {
    // full storage — ignore
  }
}

function bufferToBytes(buffer: ArrayBuffer | null): number[] {
  if (!buffer) return []
  return Array.from(new Uint8Array(buffer))
}

function base64UrlToUint8Array(base64: string): Uint8Array {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4)
  const normalized = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(normalized)
  const out = new Uint8Array(raw.length)
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i)
  return out
}

function browserSupportsPush(): boolean {
  if (typeof navigator === 'undefined') return false
  if (!('serviceWorker' in navigator)) return false
  if (typeof window === 'undefined' || !('PushManager' in window)) return false
  return true
}

export function useWebPushState(): UseWebPushStateResult {
  const [status, setStatus] = useState<WebPushStatus>('loading')
  const [categories, setCategories] = useState<Set<CitizenPushCategory>>(
    new Set(ALL_CATEGORIES)
  )
  const [vapidKey, setVapidKey] = useState<string | null>(null)
  const [endpoint, setEndpoint] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    if (!browserSupportsPush()) {
      setStatus('unsupported')
      return
    }
    const key = await getVapidKey()
    if (!key) {
      setStatus('unsupported')
      return
    }
    setVapidKey(key)
    const reg = await navigator.serviceWorker.getRegistration()
    const existing = (await reg?.pushManager.getSubscription()) ?? null
    if (existing) {
      setEndpoint(existing.endpoint)
      setCategories(loadCategories(existing.endpoint))
      setStatus(
        (globalThis.Notification?.permission ?? 'default') === 'granted'
          ? 'subscribed'
          : 'unregistered'
      )
      return
    }
    const perm = globalThis.Notification?.permission ?? 'default'
    setStatus(perm === 'denied' ? 'denied' : 'unregistered')
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const subscribe = useCallback(
    async (chosen: Set<CitizenPushCategory>) => {
      if (!vapidKey) return
      const perm = await globalThis.Notification.requestPermission()
      if (perm !== 'granted') {
        setStatus(perm === 'denied' ? 'denied' : 'unregistered')
        return
      }
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: base64UrlToUint8Array(vapidKey)
      })
      const p256dh = bufferToBytes(sub.getKey('p256dh'))
      const auth = bufferToBytes(sub.getKey('auth'))
      await putSubscription({
        endpoint: sub.endpoint,
        ecdhKey: p256dh,
        authSecret: auth,
        enabledCategories: Array.from(chosen),
        userAgent: navigator.userAgent
      })
      setEndpoint(sub.endpoint)
      setCategories(chosen)
      persistCategories(sub.endpoint, chosen)
      setStatus('subscribed')
    },
    [vapidKey]
  )

  const updateCategories = useCallback(
    async (chosen: Set<CitizenPushCategory>) => {
      if (!endpoint) return
      const reg = await navigator.serviceWorker.getRegistration()
      const sub = await reg?.pushManager.getSubscription()
      if (!sub) return
      const p256dh = bufferToBytes(sub.getKey('p256dh'))
      const auth = bufferToBytes(sub.getKey('auth'))
      await putSubscription({
        endpoint: sub.endpoint,
        ecdhKey: p256dh,
        authSecret: auth,
        enabledCategories: Array.from(chosen),
        userAgent: navigator.userAgent
      })
      setCategories(chosen)
      persistCategories(endpoint, chosen)
    },
    [endpoint]
  )

  const unsubscribe = useCallback(async () => {
    const reg = await navigator.serviceWorker.getRegistration()
    const sub = await reg?.pushManager.getSubscription()
    if (sub) {
      await deleteSubscription(sub.endpoint)
      await sub.unsubscribe()
    }
    setEndpoint(null)
    setStatus('unregistered')
  }, [])

  const sendTest = useCallback(async () => {
    await postTest()
  }, [])

  return {
    status,
    categories,
    subscribe,
    updateCategories,
    unsubscribe,
    sendTest
  }
}
