// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AdRole } from '../types'

type Layout<Components> = { component: keyof Components; open: boolean }[]
type Layouts<Components> = {
  [k in AdRole]?: Layout<Components>
}

export type LayoutsWithDefault<Components> = Layouts<Components> & {
  default: Layout<Components>
}

export const getLayout = <Components>(
  layouts: LayoutsWithDefault<Components>,
  roles: AdRole[]
): Layout<Components> => {
  // Compute the layout as union of all component names the user may have access to.
  // For example if the user is both FINANCE_ADMIN and SPECIAL_EDUCATION teacher, we display
  // components accessible by either role.
  const unionOfComponentNames = new Set(
    roles.flatMap((role) => layouts[role]?.map((part) => part.component) || [])
  )
  return (layouts['ADMIN'] || layouts['default']).filter((layout) =>
    unionOfComponentNames.has(layout.component)
  )
}
