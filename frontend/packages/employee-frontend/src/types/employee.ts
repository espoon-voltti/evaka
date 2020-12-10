// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '~/types'

export interface Employee {
  id: UUID
  externalId: string | null
  firstName: string | null
  lastName: string | null
  email: string
  created: Date
  updated: Date
}
