// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useSyncExternalStore } from 'react'

/**
 * Absent from lib.dom.d.ts, because only Chromium implements it.
 */
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

const subscribers = new Set<() => void>()

const subscribe = (onChange: () => void) => {
  subscribers.add(onChange)
  return () => {
    subscribers.delete(onChange)
  }
}

const notify = () => subscribers.forEach((onChange) => onChange())

let deferredEvent: BeforeInstallPromptEvent | undefined = undefined
const getDeferredEvent = () => deferredEvent

/**
 * Must run before React mounts: the browser fires beforeinstallprompt once,
 * while the page is still loading, and a listener added later never sees it.
 */
export function listenForInstallPrompt(): void {
  window.addEventListener('beforeinstallprompt', (event) => {
    // Keeps the browser from showing its own install UI, so the app can offer
    // the prompt at a point of its own choosing instead.
    event.preventDefault()
    deferredEvent = event as BeforeInstallPromptEvent
    notify()
  })
  window.addEventListener('appinstalled', () => {
    deferredEvent = undefined
    notify()
  })
}

export interface InstallPrompt {
  available: boolean
  show: () => Promise<'accepted' | 'dismissed'>
}

let promptOpen = false

export function useInstallPrompt(): InstallPrompt {
  const event = useSyncExternalStore(subscribe, getDeferredEvent)
  return {
    available: event !== undefined,
    show: async () => {
      if (!event || promptOpen) return 'dismissed'
      promptOpen = true
      try {
        await event.prompt()
        const { outcome } = await event.userChoice
        return outcome
      } finally {
        // The browser refuses to open the same event a second time, whichever way
        // the citizen answered, and fires a new beforeinstallprompt event instead
        deferredEvent = undefined
        promptOpen = false
        notify()
      }
    }
  }
}
