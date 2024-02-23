// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FamilyOverviewIncome } from 'lib-common/generated/api-types/pis'

export type FamilyOverviewPersonRole = 'HEAD' | 'PARTNER' | 'CHILD'

export interface FamilyOverviewRow {
  personId: string
  name: string
  role: FamilyOverviewPersonRole
  age: number
  restrictedDetailsEnabled: boolean
  address: string
  income: FamilyOverviewIncome | null
}
