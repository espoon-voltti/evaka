// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useRef, useState } from 'react'

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

export function usePwaInstall(): PwaInstallState {
  const deferredPromptRef = useRef<BeforeInstallPromptEvent | null>(null)

  const [state, setState] = useState<PwaInstallState>(() => {
    if (isStandalone()) return { kind: 'standalone' }
    return { kind: 'fallback', platform: detectPlatform() }
  })

  const promptInstall = useCallback(async () => {
    const deferred = deferredPromptRef.current
    if (!deferred) return
    await deferred.prompt()
    try {
      await deferred.userChoice
    } finally {
      deferredPromptRef.current = null
    }
  }, [])

  useEffect(() => {
    const onBeforeInstallPrompt = (event: Event) => {
      event.preventDefault()
      deferredPromptRef.current = event as BeforeInstallPromptEvent
      setState({ kind: 'native', promptInstall })
    }

    const onAppInstalled = () => {
      deferredPromptRef.current = null
      setState({ kind: 'standalone' })
    }

    window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
    window.addEventListener('appinstalled', onAppInstalled)

    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
      window.removeEventListener('appinstalled', onAppInstalled)
    }
  }, [promptInstall])

  return state
}
