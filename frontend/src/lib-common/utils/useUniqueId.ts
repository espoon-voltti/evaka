import { useMemo } from 'react'

let nextId = 1

/**
 * A hook that returns a memoized unique string id.
 *
 * @param prefix optional prefix to improve debugging (does not affect uniqueness)
 */
export function useUniqueId(prefix = 'evaka'): string {
  return useMemo(() => `${prefix}-${nextId++}`, [prefix])
}
