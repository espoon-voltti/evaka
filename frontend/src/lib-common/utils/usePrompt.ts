// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { History } from 'history'
import { ContextType, useContext, useEffect } from 'react'
import { UNSAFE_NavigationContext } from 'react-router-dom'

/**
 * Minimal replacement for react-router Prompt that was removed in v6
 */
export default function usePrompt(message: string, when = true) {
  const { navigator } = useContext(
    UNSAFE_NavigationContext
  ) as NavigationContext

  useEffect(() => {
    if (!when) return
    const unblock = navigator.block(({ retry }) => {
      const response = window.confirm(message)
      if (response) {
        unblock()
        retry()
      }
    })
    return unblock
  }, [when, navigator, message])
}

type NavigationContext = ContextType<typeof UNSAFE_NavigationContext> & {
  navigator: History
}
