// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useRef } from 'react'

export const useCloseOnOutsideEvent = (isOpen: boolean, close: () => void) => {
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!isOpen) {
      return
    }

    const handleEvent = (event: PointerEvent | FocusEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        close()
      }
    }

    document.addEventListener('pointerdown', handleEvent)
    document.addEventListener('focusin', handleEvent)

    return () => {
      document.removeEventListener('pointerdown', handleEvent)
      document.removeEventListener('focusin', handleEvent)
    }
  }, [isOpen, close])

  return containerRef
}
