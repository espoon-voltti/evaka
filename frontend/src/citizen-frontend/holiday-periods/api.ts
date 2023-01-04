// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  deserializeActiveQuestionnaire,
  deserializeHolidayPeriod
} from 'lib-common/api-types/holiday-period'
import {
  ActiveQuestionnaire,
  FixedPeriodsBody,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../api-client'

export function getHolidayPeriods(): Promise<HolidayPeriod[]> {
  return client
    .get<JsonOf<HolidayPeriod[]>>(`/citizen/holiday-period`)
    .then((res) => res.data.map(deserializeHolidayPeriod))
}

export function getActiveQuestionnaires(): Promise<ActiveQuestionnaire[]> {
  return client
    .get<JsonOf<ActiveQuestionnaire[]>>(`/citizen/holiday-period/questionnaire`)
    .then((res) => res.data.map(deserializeActiveQuestionnaire))
}

export async function postFixedPeriodQuestionnaireAnswer({
  id,
  body
}: {
  id: UUID
  body: FixedPeriodsBody
}): Promise<void> {
  return client
    .post(`/citizen/holiday-period/questionnaire/fixed-period/${id}`, body)
    .then(() => undefined)
}
