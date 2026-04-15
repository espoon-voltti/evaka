// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'

import { detectPlatform, type Platform } from './detectPlatform'

interface BeforeInstallPromptEvent extends Event {
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed'
    platform: string
  }>
  prompt(): Promise<void>
}

export type PwaInstallState =
  | { kind: 'standalone' }
  | { kind: 'native'; promptInstall: () => Promise<void> }
  | { kind: 'fallback'; platform: Platform }

const isStandalone = (): boolean => {
  if (typeof window === 'undefined') return false
  if (window.matchMedia('(display-mode: standalone)').matches) return true
  // Older iOS Safari exposes a non-standard `navigator.standalone` flag.
  const nav = window.navigator as Navigator & { standalone?: boolean }
  return nav.standalone === true
}

// `beforeinstallprompt` fires once per page load, typically very early — before
// the user has navigated to any hook consumer. We capture it at module scope so
// it survives SPA navigation (e.g. from /login to /personal-details) and all
// usePwaInstall callers see the same deferred prompt.
let deferredPrompt: BeforeInstallPromptEvent | null = null
let captured = false
let appInstalled = false
const subscribers = new Set<() => void>()

const notify = () => {
  for (const s of subscribers) s()
}

if (typeof window !== 'undefined') {
  window.addEventListener('beforeinstallprompt', (event) => {
    // Suppress the default mini-infobar so we can defer the prompt to our own UI.
    event.preventDefault()
    deferredPrompt = event as BeforeInstallPromptEvent
    captured = true
    notify()
  })
  window.addEventListener('appinstalled', () => {
    deferredPrompt = null
    appInstalled = true
    notify()
  })
}

export function usePwaInstall(): PwaInstallState {
  const computeState = useCallback((): PwaInstallState => {
    if (appInstalled || isStandalone()) return { kind: 'standalone' }
    if (captured && deferredPrompt) {
      return { kind: 'native', promptInstall }
    }
    return { kind: 'fallback', platform: detectPlatform() }
  }, [])

  const [state, setState] = useState<PwaInstallState>(computeState)

  useEffect(() => {
    const update = () => setState(computeState())
    subscribers.add(update)
    // Re-sync on mount in case the module-level flags changed between the
    // initial useState call and the effect running (e.g. StrictMode remount).
    update()
    return () => {
      subscribers.delete(update)
    }
  }, [computeState])

  return state
}

async function promptInstall(): Promise<void> {
  const deferred = deferredPrompt
  if (!deferred) return
  await deferred.prompt()
  try {
    const { outcome } = await deferred.userChoice
    if (outcome === 'accepted') {
      appInstalled = true
    }
  } finally {
    deferredPrompt = null
    captured = false
    notify()
  }
}
