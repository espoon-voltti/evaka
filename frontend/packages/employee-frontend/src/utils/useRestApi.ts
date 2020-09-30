// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ApiFunction,
  ApiResultOf,
  isCancelled,
  Loading,
  Result,
  withStaleCancellation
} from '~api'
import { useCallback, useState } from 'react'

export function useRestApi<F extends ApiFunction>(
  f: F,
  setState: (result: Result<ApiResultOf<F>>) => void
): (...args: Parameters<F>) => void {
  const [api] = useState(() => withStaleCancellation(f))
  return useCallback(
    (...args: Parameters<F>) => {
      setState(Loading())
      void api(...args).then((result) => {
        if (isCancelled(result)) return
        setState(result)
      })
    },
    [api, setState]
  )
}
