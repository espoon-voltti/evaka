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
  return (
    (roles.includes('ADMIN')
      ? layouts['ADMIN']
      : roles.includes('FINANCE_ADMIN')
      ? layouts['FINANCE_ADMIN']
      : roles.includes('STAFF')
      ? layouts['STAFF']
      : roles.includes('SPECIAL_EDUCATION_TEACHER')
      ? layouts['SPECIAL_EDUCATION_TEACHER']
      : layouts.default) ?? layouts.default
  )
}
