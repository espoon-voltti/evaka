// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getEmployeePreferredFirstName,
  setEmployeePreferredFirstName
} from '../../generated/api-clients/pis'

const q = new Queries()

export const employeePreferredFirstNameQuery = q.query(
  getEmployeePreferredFirstName
)

export const setEmployeePreferredFirstNameMutation = q.mutation(
  setEmployeePreferredFirstName,
  [employeePreferredFirstNameQuery]
)
