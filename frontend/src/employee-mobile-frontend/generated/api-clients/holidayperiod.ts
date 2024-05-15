// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { FixedPeriodQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { FixedPeriodQuestionnaireBody } from 'lib-common/generated/api-types/holidayperiod'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonFixedPeriodQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { uri } from 'lib-common/uri'


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
