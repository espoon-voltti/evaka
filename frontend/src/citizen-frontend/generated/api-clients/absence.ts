// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AbsenceApplicationCreateRequest } from 'lib-common/generated/api-types/absence'
import type { AbsenceApplicationId } from 'lib-common/generated/api-types/shared'
import type { AbsenceApplicationSummaryCitizen } from 'lib-common/generated/api-types/absence'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAbsenceApplicationSummaryCitizen } from 'lib-common/generated/api-types/absence'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerCitizen.deleteAbsenceApplication
*/
export async function deleteAbsenceApplication(
  request: {
    id: AbsenceApplicationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/absence-application/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerCitizen.getAbsenceApplicationPossibleDateRanges
*/
export async function getAbsenceApplicationPossibleDateRanges(
  request: {
    childId: PersonId
  }
): Promise<FiniteDateRange[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<FiniteDateRange[]>>({
    url: uri`/citizen/absence-application/application-possible`.toString(),
    method: 'GET',
    params
  })
  return json.map((x) => FiniteDateRange.parseJson(x))
}


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerCitizen.getAbsenceApplications
*/
export async function getAbsenceApplications(
  request: {
    childId: PersonId
  }
): Promise<AbsenceApplicationSummaryCitizen[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<AbsenceApplicationSummaryCitizen[]>>({
    url: uri`/citizen/absence-application`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonAbsenceApplicationSummaryCitizen(e))
}


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerCitizen.postAbsenceApplication
*/
export async function postAbsenceApplication(
  request: {
    body: AbsenceApplicationCreateRequest
  }
): Promise<AbsenceApplicationId> {
  const { data: json } = await client.request<JsonOf<AbsenceApplicationId>>({
    url: uri`/citizen/absence-application`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AbsenceApplicationCreateRequest>
  })
  return json
}
