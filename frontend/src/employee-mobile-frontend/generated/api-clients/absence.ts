// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { Absence } from 'lib-common/generated/api-types/absence'
import { AxiosHeaders } from 'axios'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonAbsence } from 'lib-common/generated/api-types/absence'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.absence.AbsenceControllerEmployeeMobile.futureAbsencesOfChild
*/
export async function futureAbsencesOfChild(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<Absence[]> {
  const { data: json } = await client.request<JsonOf<Absence[]>>({
    url: uri`/employee-mobile/absences/by-child/${request.childId}/future`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonAbsence(e))
}
