// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ActiveQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { FixedPeriodsBody } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { deserializeJsonActiveQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { deserializeJsonHolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodControllerCitizen.answerFixedPeriodQuestionnaire
*/
export async function answerFixedPeriodQuestionnaire(
  request: {
    id: UUID,
    body: FixedPeriodsBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/holiday-period/questionnaire/fixed-period/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FixedPeriodsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodControllerCitizen.getActiveQuestionnaires
*/
export async function getActiveQuestionnaires(): Promise<ActiveQuestionnaire[]> {
  const { data: json } = await client.request<JsonOf<ActiveQuestionnaire[]>>({
    url: uri`/citizen/holiday-period/questionnaire`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonActiveQuestionnaire(e))
}


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodControllerCitizen.getHolidayPeriods
*/
export async function getHolidayPeriods(): Promise<HolidayPeriod[]> {
  const { data: json } = await client.request<JsonOf<HolidayPeriod[]>>({
    url: uri`/citizen/holiday-period`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonHolidayPeriod(e))
}
