// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useRef, useState } from 'react'
import {
  ApiFunction,
  ApiResultOf,
  isCancelled,
  Loading,
  Result,
  withStaleCancellation
} from '../api'

export function useRestApi<F extends ApiFunction>(
  f: F,
  setState: (result: Result<ApiResultOf<F>>) => void,
  keepStale?: boolean
): (...args: Parameters<F>) => void {
  const [api] = useState(() => withStaleCancellation(f))
  const mountedRef = useRef(true)

  return useCallback(
    (...args: Parameters<F>) => {
      if (!keepStale) setState(Loading.of())
      void api(...args).then((result) => {
        if (!mountedRef.current || isCancelled(result)) return
        setState(result)
      })
    },
    [api, setState, keepStale]
  )
}
