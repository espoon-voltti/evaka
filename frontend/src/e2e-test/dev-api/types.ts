// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevPerson } from '../generated/api-types'

export interface Family {
  guardian: DevPerson
  otherGuardian?: DevPerson
  children: DevPerson[]
}
