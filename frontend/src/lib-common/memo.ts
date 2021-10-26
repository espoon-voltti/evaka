// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

export enum MemoizedBrand {
  _ = ''
}

type Memoized<T> = T extends
  | string
  | number
  | boolean
  | undefined
  | null
  | symbol
  ? T
  : T & MemoizedBrand

// eslint-disable-next-line @typescript-eslint/ban-types
type MemoizedProps<P extends object> = {
  [K in keyof P]: Memoized<P[K]>
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function memo<T extends object>(
  component: React.FunctionComponent<MemoizedProps<T>>
) {
  return React.memo(component)
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function useCallback<T extends (...args: any[]) => any>(
  callback: T,
  deps: React.DependencyList
): Memoized<T> {
  // eslint-disable-next-line react-hooks/exhaustive-deps
  return React.useCallback(callback, deps) as Memoized<T>
}

export function useMemo<T>(
  factory: () => T,
  deps: React.DependencyList | undefined
): Memoized<T> {
  // eslint-disable-next-line react-hooks/exhaustive-deps
  return React.useMemo(factory, deps) as Memoized<T>
}

export function useState<T>(
  initialState: T | (() => T)
): [Memoized<T>, Memoized<React.Dispatch<React.SetStateAction<T>>>] {
  return React.useState(initialState) as [
    Memoized<T>,
    Memoized<React.Dispatch<React.SetStateAction<T>>>
  ]
}
