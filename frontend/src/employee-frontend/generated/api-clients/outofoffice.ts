// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonOf } from 'lib-common/json'
import { OutOfOfficeId } from 'lib-common/generated/api-types/shared'
import { OutOfOfficePeriod } from 'lib-common/generated/api-types/outofoffice'
import { client } from '../../api/client'
import { deserializeJsonOutOfOfficePeriod } from 'lib-common/generated/api-types/outofoffice'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.outofoffice.OutOfOfficeController.deleteOutOfOfficePeriod
*/
export async function deleteOutOfOfficePeriod(
  request: {
    id: OutOfOfficeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/out-of-office/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.outofoffice.OutOfOfficeController.getOutOfOfficePeriods
*/
export async function getOutOfOfficePeriods(): Promise<OutOfOfficePeriod[]> {
  const { data: json } = await client.request<JsonOf<OutOfOfficePeriod[]>>({
    url: uri`/employee/out-of-office`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonOutOfOfficePeriod(e))
}


/**
* Generated from fi.espoo.evaka.outofoffice.OutOfOfficeController.upsertOutOfOfficePeriod
*/
export async function upsertOutOfOfficePeriod(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/out-of-office`.toString(),
    method: 'POST'
  })
  return json
}
