// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DependencyList, useCallback, useEffect, useRef, useState } from 'react'

import {
  ApiFunction,
  ApiResultOf,
  Failure,
  isCancelled,
  Loading,
  Result,
  withStaleCancellation
} from '../api'

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

/**
 * Store the result of an API endpoint in component state.
 *
 * The data is automatically fetched on first mount, and re-fetched every time
 * `f` or `args` change. The data is also re-fetched if you manually call the
 * returned `reload` function.
 *
 * During the initial fetch, the `result` is in the `Loading` state. During
 * re-fetches, it is in the `Success/isReloading` state.
 *
 * @param f     The async function to call to fetch data
 * @param deps  The arguments to pass to the function
 * @returns [result, reload] `result` is a `Result`, and `reload` is a function to reload the same data.
 *
 * @deprecated Use `useQueryResult()` instead
 */
export function useApiState<T, Deps extends DependencyList>(
  f: () => Promise<Result<T>>,
  deps: Deps
): [Result<T>, () => Promise<Result<T>>] {
  const [state, setState] = useState<Result<T>>(Loading.of())

  const currentRef = useRef(0)
  useEffect(
    () => () => {
      currentRef.current = -1
    },
    []
  )

  const load = useCallback(async () => {
    const api = withStaleCancellation(f)
    setState((prev) => (prev.isSuccess ? prev.reloading() : Loading.of()))
    const loadIdx = ++currentRef.current
    return await api().then<Result<T>>((result) => {
      if (currentRef.current === loadIdx && !isCancelled(result)) {
        setState(result)
        return result
      }

      return Failure.of({
        message: 'Cancelled'
      })
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    void load()
  }, [load])

  return [state, load]
}
