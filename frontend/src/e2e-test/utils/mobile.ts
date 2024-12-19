// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId, EmployeeId } from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'

import config from '../config'
import { uuidv4 } from '../dev-api/fixtures'
import {
  postMobileDevice,
  postPersonalMobileDevice
} from '../generated/api-clients'

/** Create a mobile device for the given employee and unit
 *
 * @return An URL that completes the pairing in the browser
 */
export async function pairMobileDevice(unitId: DaycareId): Promise<string> {
  const longTermToken = uuidv4()
  await postMobileDevice({
    body: {
      id: randomId(),
      unitId,
      name: 'testMobileDevice',
      longTermToken,
      pushNotificationCategories: []
    }
  })
  return `${config.mobileBaseUrl}/api/dev-api/auth/mobile-e2e-signup?token=${longTermToken}`
}

/** Create a personal mobile device for the given employee
 *
 * @return An URL that completes the pairing in the browser
 */
export async function pairPersonalMobileDevice(
  employeeId: EmployeeId
): Promise<string> {
  const longTermToken = uuidv4()
  await postPersonalMobileDevice({
    body: {
      id: randomId(),
      employeeId,
      name: 'testMobileDevice',
      longTermToken
    }
  })
  return `${config.mobileBaseUrl}/api/dev-api/auth/mobile-e2e-signup?token=${longTermToken}`
}
