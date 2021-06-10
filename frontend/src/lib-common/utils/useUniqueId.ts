// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
