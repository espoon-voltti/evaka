// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import { postMobileDevice } from 'e2e-test-common/dev-api'
import { uuidv4 } from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'

/** Create a mobile device for the given employee and unit
 *
 * @return An URL that completes the pairing in the browser
 */
export async function pairMobileDevice(
  employeeId: UUID,
  unitId: UUID
): Promise<string> {
  const longTermToken = uuidv4()
  await postMobileDevice({
    id: employeeId,
    unitId,
    name: 'testMobileDevice',
    deleted: false,
    longTermToken
  })
  return `${config.mobileBaseUrl}/api/internal/auth/mobile-e2e-signup?token=${longTermToken}`
}
