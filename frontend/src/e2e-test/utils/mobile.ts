// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../config'
import { postMobileDevice, postPersonalMobileDevice } from '../dev-api'
import { uuidv4 } from '../dev-api/fixtures'

/** Create a mobile device for the given employee and unit
 *
 * @return An URL that completes the pairing in the browser
 */
export async function pairMobileDevice(unitId: UUID): Promise<string> {
  const longTermToken = uuidv4()
  await postMobileDevice({
    id: uuidv4(),
    unitId,
    name: 'testMobileDevice',
    longTermToken
  })
  return `${config.mobileBaseUrl}/api/internal/auth/mobile-e2e-signup?token=${longTermToken}`
}

/** Create a personal mobile device for the given employee
 *
 * @return An URL that completes the pairing in the browser
 */
export async function pairPersonalMobileDevice(
  employeeId: UUID
): Promise<string> {
  const longTermToken = uuidv4()
  await postPersonalMobileDevice({
    id: uuidv4(),
    employeeId,
    name: 'testMobileDevice',
    longTermToken
  })
  return `${config.mobileBaseUrl}/api/internal/auth/mobile-e2e-signup?token=${longTermToken}`
}
