// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmployeeMobileCustomizations } from 'lib-customizations/types'

import { appConfig } from './appConfigs'
import featureFlags from './featureFlags'
import { additionalStaffAttendanceTypes } from './shared'

const customizations: EmployeeMobileCustomizations = {
  appConfig,
  featureFlags,
  translations: {
    fi: {}
  },
  additionalStaffAttendanceTypes
}

export default customizations
