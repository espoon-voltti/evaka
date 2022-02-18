// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DependencyList, useCallback, useEffect, useRef, useState } from 'react'

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
  setState: (result: Result<ApiResultOf<F>>) => void
): (...args: Parameters<F>) => void {
  const [api] = useState(() => withStaleCancellation(f))
  const mountedRef = useRef(true)

  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  return useCallback(
    (...args: Parameters<F>) => {
      setState(Loading.of())
      void api(...args).then((result) => {
        if (!mountedRef.current || isCancelled(result)) return
        setState(result)
      })
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
 */
export function useApiState<T, Deps extends DependencyList>(
  f: () => Promise<Result<T>>,
  deps: Deps
): [Result<T>, () => void] {
  const [state, setState] = useState<Result<T>>(Loading.of())

  const mountedRef = useRef(true)
  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  const load = useCallback(() => {
    const api = withStaleCancellation(f)
    setState((prev) => (prev.isSuccess ? prev.reloading() : Loading.of()))
    void api().then((result) => {
      if (mountedRef.current && !isCancelled(result)) setState(result)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(load, [load])

  return [state, load]
}
