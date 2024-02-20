// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { FixedPeriodQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { FixedPeriodQuestionnaireBody } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayPeriodBody } from 'lib-common/generated/api-types/holidayperiod'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonFixedPeriodQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { deserializeJsonHolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.createHolidayPeriod
*/
export async function createHolidayPeriod(
  request: {
    body: HolidayPeriodBody
  }
): Promise<HolidayPeriod> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod>>({
    url: uri`/holiday-period`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<HolidayPeriodBody>
  })
  return deserializeJsonHolidayPeriod(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.deleteHolidayPeriod
*/
export async function deleteHolidayPeriod(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/holiday-period/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.getHolidayPeriod
*/
export async function getHolidayPeriod(
  request: {
    id: UUID
  }
): Promise<HolidayPeriod> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod>>({
    url: uri`/holiday-period/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonHolidayPeriod(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.getHolidayPeriods
*/
export async function getHolidayPeriods(): Promise<HolidayPeriod[]> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod[]>>({
    url: uri`/holiday-period`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonHolidayPeriod(e))
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.updateHolidayPeriod
*/
export async function updateHolidayPeriod(
  request: {
    id: UUID,
    body: HolidayPeriodBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/holiday-period/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<HolidayPeriodBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.createHolidayQuestionnaire
*/
export async function createHolidayQuestionnaire(
  request: {
    body: FixedPeriodQuestionnaireBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/holiday-period/questionnaire`.toString(),
    method: 'POST',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.getQuestionnaire
*/
export async function getQuestionnaire(
  request: {
    id: UUID
  }
): Promise<FixedPeriodQuestionnaire> {
  const { data: json } = await client.request<JsonOf<FixedPeriodQuestionnaire>>({
    url: uri`/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonFixedPeriodQuestionnaire(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.getQuestionnaires
*/
export async function getQuestionnaires(): Promise<FixedPeriodQuestionnaire[]> {
  const { data: json } = await client.request<JsonOf<FixedPeriodQuestionnaire[]>>({
    url: uri`/holiday-period/questionnaire`.toString(),
    method: 'GET'
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FixedPeriodQuestionnaireBody>
  })
  return json
}
