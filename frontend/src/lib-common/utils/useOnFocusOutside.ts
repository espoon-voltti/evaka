// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback } from 'react'
import type { FocusEvent } from 'react'

export const useOnFocusOutside = (action: () => void) => {
  return useCallback(
    (event: FocusEvent) => {
      if (!event.currentTarget.contains(event.relatedTarget)) {
        action()
      }
    },
    [action]
  )
}
