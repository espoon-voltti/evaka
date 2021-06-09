// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GlobalRole, ScopedRole, UUID } from './index'

export interface Employee {
  id: UUID
  externalId: string | null
  firstName: string | null
  lastName: string | null
  email: string | null
  created: Date
  updated: Date
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
