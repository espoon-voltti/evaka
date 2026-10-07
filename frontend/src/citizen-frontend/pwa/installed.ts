// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useLayoutEffect, useSyncExternalStore } from 'react'

import { isAutomatedTest } from 'lib-common/utils/helpers'

const standaloneQuery = () => window.matchMedia('(display-mode: standalone)')

const subscribe = (onChange: () => void) => {
  const query = standaloneQuery()
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

const isRunningInstalled = () =>
  standaloneQuery().matches ||
  // iOS Safari does not implement the display-mode media feature, so the home
  // screen app is only recognisable through this non-standard property.
  ('standalone' in navigator && navigator.standalone === true) ||
  (isAutomatedTest && window.evaka?.forceStandalone === true)

export function useIsRunningInstalled(): boolean {
  return useSyncExternalStore(subscribe, isRunningInstalled)
}

/**
 * Sets up the document for the app shell layout (see App.tsx) while the app
 * runs installed.
 */
export function useStandaloneLayout() {
  const runningInstalled = useIsRunningInstalled()

  // Styles target the app shell layout with `html[data-standalone] &`
  useLayoutEffect(() => {
    document.documentElement.toggleAttribute(
      'data-standalone',
      runningInstalled
    )
  }, [runningInstalled])

  // The app shell fills the document exactly, the document never scrolls.
  // However, at least iOS 27 seems to have a bug where the document does
  // scroll when switching between landscape and portrait orientation. This
  // effect resets the scroll position after that happens.
  useEffect(() => {
    if (!runningInstalled) return

    const resetScroll = () => {
      // While the on-screen keyboard is open, iOS scrolls the document to keep
      // the focused field visible
      const focused = document.activeElement
      if (
        focused instanceof HTMLInputElement ||
        focused instanceof HTMLTextAreaElement ||
        (focused instanceof HTMLElement && focused.isContentEditable)
      ) {
        return
      }

      if (window.scrollY !== 0) window.scrollTo(0, 0)
    }

    window.addEventListener('scroll', resetScroll)
    window.visualViewport?.addEventListener('resize', resetScroll)
    return () => {
      window.removeEventListener('scroll', resetScroll)
      window.visualViewport?.removeEventListener('resize', resetScroll)
    }
  }, [runningInstalled])
}
