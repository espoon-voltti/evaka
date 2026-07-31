// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useState } from 'react'

import { useBoolean } from 'lib-common/form/hooks'

// Hook for announcing messages to screen reader users
// Needs the ariaLiveBustingValue trick to make sure repeated messages are read out,
// e.g. Mac VoiceOver seems to cache the content otherwise.
// Needs the message to be cleared after timeout to prevent screen readers from reading it out again.
// Works best with aria-live set to "polite"
export const useScreenReaderMessage = (): [
  string | null,
  (message: string) => void
] => {
  const [screenReaderMessage, setScreenReaderMessage] = useState<string | null>(
    null
  )
  const [isMessageTimerOn, setIsMessageTimerOn] = useState(false)
  const [ariaLiveBustingValue, { toggle }] = useBoolean(false)
  const showScreenReaderMessage = useCallback(
    (message: string) => {
      if (isMessageTimerOn) return
      toggle()
      setScreenReaderMessage(message + (ariaLiveBustingValue ? '​' : ''))
      setIsMessageTimerOn(true)
      setTimeout(() => {
        setScreenReaderMessage(null)
        setIsMessageTimerOn(false)
      }, 1000)
    },
    [ariaLiveBustingValue, isMessageTimerOn, toggle]
  )
  return [screenReaderMessage, showScreenReaderMessage]
}
