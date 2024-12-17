// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useState } from 'react'

export type Checked<T extends string> = Partial<Record<T, boolean | undefined>>

export function useCheckedState<T extends string>() {
  const [checked, setChecked] = useState<Checked<T>>({} as Checked<T>)
  const toggleChecked = (id: T) =>
    setChecked({
      ...checked,
      [id]: !checked[id]
    })
  const checkIds = (ids: T[]) => {
    const idsChecked = ids.map((id) => [id, true] as const)
    setChecked({
      ...checked,
      ...Object.fromEntries(idsChecked)
    })
  }
  const clearChecked = () => setChecked({} as Checked<T>)
  const isChecked = (id: T) => checked[id] ?? false
  const getCheckedIds = () =>
    Object.entries(checked).flatMap(([id, checked]) =>
      checked ? [id as T] : []
    )

  return { isChecked, getCheckedIds, toggleChecked, checkIds, clearChecked }
}
