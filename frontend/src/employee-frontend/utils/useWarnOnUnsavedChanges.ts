// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

/**
 * Show a warning dialog when the user tries to navigate away from the page or refresh it.
 * Note: This does *not* block back/forward buttons.
 */
export function useWarnOnUnsavedChanges(dirty: boolean, warningText: string) {
  useEffect(() => {
    if (!dirty) return undefined

    const beforeUnloadHandler = (e: BeforeUnloadEvent) => {
      // Support different browsers: https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event
      e.preventDefault()
      e.returnValue = warningText
      return warningText
    }
    window.addEventListener('beforeunload', beforeUnloadHandler)

    // monkey patch history.pushState and history.replaceState to prevent navigation
    const restorePushState = preventNavigation('pushState', warningText)
    const restoreReplaceState = preventNavigation('replaceState', warningText)

    return () => {
      restorePushState()
      restoreReplaceState()
      window.removeEventListener('beforeunload', beforeUnloadHandler)
    }
  }, [dirty, warningText])
}

// The only way is to monkey patch the history API methods
function preventNavigation(
  fn: 'pushState' | 'replaceState',
  warningText: string
) {
  const originalFn = history[fn]
  history[fn] = (...args) => {
    if (confirm(warningText)) {
      originalFn.apply(history, args)
    }
  }
  return () => {
    history[fn] = originalFn
  }
}
