// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { DailyServiceTimeId } from 'lib-common/generated/api-types/shared'
import { DailyServiceTimesEndDate } from 'lib-common/generated/api-types/dailyservicetimes'
import { DailyServiceTimesResponse } from 'lib-common/generated/api-types/dailyservicetimes'
import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { deserializeJsonDailyServiceTimesResponse } from 'lib-common/generated/api-types/dailyservicetimes'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.deleteDailyServiceTimes
*/
export async function deleteDailyServiceTimes(
  request: {
    id: DailyServiceTimeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daily-service-times/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.getDailyServiceTimes
*/
export async function getDailyServiceTimes(
  request: {
    childId: PersonId
  }
): Promise<DailyServiceTimesResponse[]> {
  const { data: json } = await client.request<JsonOf<DailyServiceTimesResponse[]>>({
    url: uri`/employee/children/${request.childId}/daily-service-times`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonDailyServiceTimesResponse(e))
}


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.postDailyServiceTimes
*/
export async function postDailyServiceTimes(
  request: {
    childId: PersonId,
    body: DailyServiceTimesValue
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/children/${request.childId}/daily-service-times`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DailyServiceTimesValue>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.putDailyServiceTimes
*/
export async function putDailyServiceTimes(
  request: {
    id: DailyServiceTimeId,
    body: DailyServiceTimesValue
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daily-service-times/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DailyServiceTimesValue>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.putDailyServiceTimesEnd
*/
export async function putDailyServiceTimesEnd(
  request: {
    id: DailyServiceTimeId,
    body: DailyServiceTimesEndDate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daily-service-times/${request.id}/end`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DailyServiceTimesEndDate>
  })
  return json
}
