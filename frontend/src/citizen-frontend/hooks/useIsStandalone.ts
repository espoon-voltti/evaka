// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

export function useIsStandalone(): boolean {
  return useMemo(() => {
    if (typeof window === 'undefined') return false
    if (window.matchMedia?.('(display-mode: standalone)').matches) return true
    if ((navigator as unknown as { standalone?: boolean }).standalone === true)
      return true
    return false
  }, [])
}
