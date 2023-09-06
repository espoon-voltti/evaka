// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CitizenModule,
  CommonModule,
  EmployeeMobileModule,
  EmployeeModule
} from '../types'

import * as citizen from './citizen'
import * as common from './common'
import * as employee from './employee'
import * as employeeMobile from './employeeMobile'

export const citizenModule: CitizenModule = citizen
export const commonModule: CommonModule = common
export const employeeModule: EmployeeModule = employee
export const employeeMobileModule: EmployeeMobileModule = employeeMobile
