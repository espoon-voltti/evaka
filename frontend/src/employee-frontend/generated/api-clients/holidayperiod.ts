// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { FixedPeriodQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { FixedPeriodQuestionnaireBody } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayPeriodCreate } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayPeriodUpdate } from 'lib-common/generated/api-types/holidayperiod'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonFixedPeriodQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { deserializeJsonHolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.createHolidayPeriod
*/
export async function createHolidayPeriod(
  request: {
    body: HolidayPeriodCreate
  },
  headers?: AxiosHeaders
): Promise<HolidayPeriod> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod>>({
    url: uri`/employee/holiday-period`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<HolidayPeriodCreate>
  })
  return deserializeJsonHolidayPeriod(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.deleteHolidayPeriod
*/
export async function deleteHolidayPeriod(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.getHolidayPeriod
*/
export async function getHolidayPeriod(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<HolidayPeriod> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod>>({
    url: uri`/employee/holiday-period/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonHolidayPeriod(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.getHolidayPeriods
*/
export async function getHolidayPeriods(
  headers?: AxiosHeaders
): Promise<HolidayPeriod[]> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod[]>>({
    url: uri`/employee/holiday-period`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonHolidayPeriod(e))
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.updateHolidayPeriod
*/
export async function updateHolidayPeriod(
  request: {
    id: UUID,
    body: HolidayPeriodUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<HolidayPeriodUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.createHolidayQuestionnaire
*/
export async function createHolidayQuestionnaire(
  request: {
    body: FixedPeriodQuestionnaireBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/questionnaire`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<FixedPeriodQuestionnaireBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.deleteHolidayQuestionnaire
*/
export async function deleteHolidayQuestionnaire(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.getQuestionnaire
*/
export async function getQuestionnaire(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<FixedPeriodQuestionnaire> {
  const { data: json } = await client.request<JsonOf<FixedPeriodQuestionnaire>>({
    url: uri`/employee/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonFixedPeriodQuestionnaire(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.getQuestionnaires
*/
export async function getQuestionnaires(
  headers?: AxiosHeaders
): Promise<FixedPeriodQuestionnaire[]> {
  const { data: json } = await client.request<JsonOf<FixedPeriodQuestionnaire[]>>({
    url: uri`/employee/holiday-period/questionnaire`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonFixedPeriodQuestionnaire(e))
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.updateHolidayQuestionnaire
*/
export async function updateHolidayQuestionnaire(
  request: {
    id: UUID,
    body: FixedPeriodQuestionnaireBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<FixedPeriodQuestionnaireBody>
  })
  return json
}
