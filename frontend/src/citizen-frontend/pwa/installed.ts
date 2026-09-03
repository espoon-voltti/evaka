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
