// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fi } from '../defaults/employee-mobile-frontend/i18n/fi'
import { EmployeeMobileModule } from '../types'

export { employeeMobileConfig as sentryConfig } from './sentryConfigs'
export { featureFlags } from './featureFlags'

export const translations: EmployeeMobileModule['translations'] = {
  fi
}
