// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useState } from 'react'
import { Checked } from './invoicing-ui'

export function useCheckedState() {
  const [checked, setChecked] = useState<Checked>({})
  const toggleChecked = (id: string) =>
    setChecked({
      ...checked,
      [id]: !checked[id]
    })
  const checkIds = (ids: string[]) => {
    const idsChecked = ids.map((id) => ({ [id]: true }))
    // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
    setChecked({
      ...checked,
      ...Object.assign({}, ...idsChecked)
    })
  }
  const clearChecked = () => setChecked({})

  return { checked, toggleChecked, checkIds, clearChecked }
}
