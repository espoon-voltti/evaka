// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useRef, useState } from 'react'

import type { ApiFunction, ApiResultOf, Result } from '../api'
import { Failure, isCancelled, Loading, withStaleCancellation } from '../api'

type APICallFn<F extends ApiFunction> = (
  ...args: Parameters<F>
) => Promise<Result<ApiResultOf<F>>>

/**
 * @deprecated Use `useQueryResult()` instead
 */
export function useRestApi<F extends ApiFunction>(
  f: F,
  setState: (result: Result<ApiResultOf<F>>) => void
): APICallFn<F> {
  const [api] = useState(() => withStaleCancellation(f))
  const mountedRef = useRef(true)

  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  return useCallback<APICallFn<F>>(
    async (...args) => {
      setState(Loading.of())

      const result = await api(...args)

      if (!mountedRef.current || isCancelled(result)) {
        return Failure.of({
          message: 'Cancelled or mountedRef does not exist'
        })
      }

      setState(result)

      return result
    },
    [api, setState]
  )
}
