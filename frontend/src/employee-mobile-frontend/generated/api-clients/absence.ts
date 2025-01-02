// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { Absence } from 'lib-common/generated/api-types/absence'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../client'
import { deserializeJsonAbsence } from 'lib-common/generated/api-types/absence'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.absence.AbsenceControllerEmployeeMobile.futureAbsencesOfChild
*/
export async function futureAbsencesOfChild(
  request: {
    childId: PersonId
  }
): Promise<Absence[]> {
  const { data: json } = await client.request<JsonOf<Absence[]>>({
    url: uri`/employee-mobile/absences/by-child/${request.childId}/future`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAbsence(e))
}
