// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AdRole } from 'lib-common/api-types/employee-auth'

type Layout<Components> = { component: keyof Components; open: boolean }[]
export type Layouts<Components> = Partial<Record<AdRole, Layout<Components>>>

export const getLayout = <Components>(
  layouts: Layouts<Components>,
  roles: AdRole[]
): Layout<Components> => {
  if (roles.length === 1) {
    return layouts[roles[0]] ?? []
  }

  // Compute the layout as union of all component names the user may have access to.
  // For example if the user is both FINANCE_ADMIN and SPECIAL_EDUCATION teacher, we display
  // components accessible by either role.
  const unionOfComponentNames = new Set(
    roles.flatMap((role) => layouts[role]?.map((part) => part.component) || [])
  )
  const allLayouts = layouts.ADMIN ?? []
  return allLayouts.filter((layout) =>
    unionOfComponentNames.has(layout.component)
  )
}
