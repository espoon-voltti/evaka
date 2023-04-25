// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { GlobalRole, ScopedRole } from 'lib-common/api-types/employee-auth'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { UUID } from 'lib-common/types'

export interface Employee {
  id: UUID
  externalId: string | null
  firstName: string | null
  lastName: string | null
  email: string | null
  created: HelsinkiDateTime
  updated: HelsinkiDateTime
}

interface DaycareRole {
  daycareId: string
  daycareName: string
  role: ScopedRole
}

export interface EmployeeUser {
  id: UUID
  firstName: string
  lastName: string
  email: string | null
  globalRoles: GlobalRole[]
  daycareRoles: DaycareRole[]
}
