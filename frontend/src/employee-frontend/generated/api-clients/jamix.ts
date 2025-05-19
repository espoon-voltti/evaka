// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.jamix.JamixController.sendJamixOrders
*/
export async function sendJamixOrders(
  request: {
    unitId: DaycareId,
    date: LocalDate
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/jamix/send-orders`.toString(),
    method: 'PUT',
    params
  })
  return json
}
