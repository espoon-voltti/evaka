// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useRef } from 'react'

type Callback = () => void

export function useInterval(cb: Callback, delayMs: number) {
  const callbackRef = useRef<Callback>(undefined)

  useEffect(() => {
    callbackRef.current = cb
  }, [cb])

  useEffect(() => {
    function functionToBeExecutedLater() {
      callbackRef?.current?.()
    }

    const id = setInterval(functionToBeExecutedLater, delayMs)
    return () => clearInterval(id)
  }, [delayMs])
}
