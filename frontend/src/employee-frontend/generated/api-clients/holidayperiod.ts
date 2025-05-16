// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import type { HolidayPeriodCreate } from 'lib-common/generated/api-types/holidayperiod'
import type { HolidayPeriodId } from 'lib-common/generated/api-types/shared'
import type { HolidayPeriodUpdate } from 'lib-common/generated/api-types/holidayperiod'
import type { HolidayQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import type { HolidayQuestionnaireId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { QuestionnaireBody } from 'lib-common/generated/api-types/holidayperiod'
import { client } from '../../api/client'
import { deserializeJsonHolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { deserializeJsonHolidayQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.createHolidayPeriod
*/
export async function createHolidayPeriod(
  request: {
    body: HolidayPeriodCreate
  }
): Promise<HolidayPeriod> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod>>({
    url: uri`/employee/holiday-period`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<HolidayPeriodCreate>
  })
  return deserializeJsonHolidayPeriod(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.deleteHolidayPeriod
*/
export async function deleteHolidayPeriod(
  request: {
    id: HolidayPeriodId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.getHolidayPeriod
*/
export async function getHolidayPeriod(
  request: {
    id: HolidayPeriodId
  }
): Promise<HolidayPeriod> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod>>({
    url: uri`/employee/holiday-period/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonHolidayPeriod(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.getHolidayPeriods
*/
export async function getHolidayPeriods(): Promise<HolidayPeriod[]> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod[]>>({
    url: uri`/employee/holiday-period`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonHolidayPeriod(e))
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodController.updateHolidayPeriod
*/
export async function updateHolidayPeriod(
  request: {
    id: HolidayPeriodId,
    body: HolidayPeriodUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<HolidayPeriodUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.createHolidayQuestionnaire
*/
export async function createHolidayQuestionnaire(
  request: {
    body: QuestionnaireBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/questionnaire`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<QuestionnaireBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.deleteHolidayQuestionnaire
*/
export async function deleteHolidayQuestionnaire(
  request: {
    id: HolidayQuestionnaireId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.getQuestionnaire
*/
export async function getQuestionnaire(
  request: {
    id: HolidayQuestionnaireId
  }
): Promise<HolidayQuestionnaire> {
  const { data: json } = await client.request<JsonOf<HolidayQuestionnaire>>({
    url: uri`/employee/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonHolidayQuestionnaire(json)
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.getQuestionnaires
*/
export async function getQuestionnaires(): Promise<HolidayQuestionnaire[]> {
  const { data: json } = await client.request<JsonOf<HolidayQuestionnaire[]>>({
    url: uri`/employee/holiday-period/questionnaire`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonHolidayQuestionnaire(e))
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireController.updateHolidayQuestionnaire
*/
export async function updateHolidayQuestionnaire(
  request: {
    id: HolidayQuestionnaireId,
    body: QuestionnaireBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/holiday-period/questionnaire/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<QuestionnaireBody>
  })
  return json
}
