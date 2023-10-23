// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation } from 'lib-common/query'

import { activateEmployee, deactivateEmployee } from '../../api/employees'

export const activateEmployeeMutation = mutation({
  api: activateEmployee
})

export const deactivateEmployeeMutation = mutation({
  api: deactivateEmployee
})
