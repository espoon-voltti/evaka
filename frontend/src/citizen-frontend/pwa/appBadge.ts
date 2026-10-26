// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

function clearAppBadge() {
  if (!('clearAppBadge' in navigator)) return
  navigator.clearAppBadge().catch(() => undefined)
}

export function useClearAppBadgeOnVisibilityChange(): void {
  useEffect(() => {
    clearAppBadge()
    document.addEventListener('visibilitychange', clearAppBadge)
    return () => document.removeEventListener('visibilitychange', clearAppBadge)
  }, [])
}
