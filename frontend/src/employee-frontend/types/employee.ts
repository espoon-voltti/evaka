// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GlobalRole, ScopedRole } from 'lib-common/api-types/employee-auth'
import { UUID } from 'lib-common/types'

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
