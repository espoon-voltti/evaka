// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useRef } from 'react'

export function useStableCallback<Args extends unknown[], Ret>(
  callback: (...args: Args) => Ret
): (...args: Args) => Ret {
  const callbackRef = useRef(callback)
  callbackRef.current = callback
  return useCallback((...args: Args) => callbackRef.current(...args), [])
}
