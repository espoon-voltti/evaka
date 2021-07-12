// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect } from 'react'

export function useWarnOnUnsavedChanges(dirty: boolean, warningText: string) {
  useEffect(() => {
    const beforeUnloadHandler = (e: BeforeUnloadEvent) => {
      if (dirty) {
        // Support different browsers: https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event
        e.preventDefault()
        e.returnValue = warningText
        return warningText
      }
      return
    }

    window.addEventListener('beforeunload', beforeUnloadHandler)
    return () => {
      window.removeEventListener('beforeunload', beforeUnloadHandler)
    }
  }, [dirty, warningText])
}
