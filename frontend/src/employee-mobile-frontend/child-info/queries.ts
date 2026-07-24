// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ChildSensitiveInformation } from 'lib-common/generated/api-types/sensitive'
import { Queries } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'

import type { PinLoginRequired } from '../auth/api'
import { mapPinLoginRequiredError } from '../auth/api'
import { getSensitiveInfo } from '../generated/api-clients/sensitive'

const q = new Queries()

async function getSensitiveInfoWithPinCheck(
  req: Arg0<typeof getSensitiveInfo>
): Promise<ChildSensitiveInformation | PinLoginRequired> {
  try {
    return await getSensitiveInfo(req)
  } catch (e) {
    const pinLoginRequired = mapPinLoginRequiredError(e)
    if (pinLoginRequired.isSuccess) {
      return pinLoginRequired.value
    }
    throw e
  }
}

export const childSensitiveInfoQuery = q.query(getSensitiveInfoWithPinCheck, {
  // Sensitive child data is PIN-gated and should not linger in the query cache.
  // `gcTime: 0` drops the cached data as soon as this view unmounts.
  // `RequirePinAuth` unmounts the subtree when the PIN session ends.
  gcTime: 0
})
