// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useSyncExternalStore } from 'react'

const standaloneQuery = () => window.matchMedia('(display-mode: standalone)')

const subscribe = (onChange: () => void) => {
  const query = standaloneQuery()
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

const getIsRunningInstalled = () =>
  standaloneQuery().matches ||
  // iOS Safari does not implement the display-mode media feature, so the home
  // screen app is only recognisable through this non-standard property.
  ('standalone' in navigator && navigator.standalone === true)

export function useIsRunningInstalled(): boolean {
  return useSyncExternalStore(subscribe, getIsRunningInstalled)
}

/**
 * Marks the document while the app runs from the home screen, so the layout can
 * be styled for it. iOS misplaces viewport positioned elements there, which the
 * installed app avoids by scrolling a container instead of the document. A
 * browser keeps scrolling the document, because that is what makes it collapse
 * its own toolbar.
 */
export function trackRunningInstalled(): void {
  const update = () =>
    document.documentElement.toggleAttribute(
      'data-standalone',
      getIsRunningInstalled()
    )
  subscribe(update)
  update()
}
