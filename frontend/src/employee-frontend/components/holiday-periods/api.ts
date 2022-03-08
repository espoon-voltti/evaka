// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  deserializeFixedPeriodQuestionnaire,
  deserializeHolidayPeriod
} from 'lib-common/api-types/holiday-period'
import {
  FixedPeriodQuestionnaire,
  FixedPeriodQuestionnaireBody,
  HolidayPeriod,
  HolidayPeriodBody
} from 'lib-common/generated/api-types/holidayperiod'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../../api/client'

export function getHolidayPeriods(): Promise<Result<HolidayPeriod[]>> {
  return client
    .get<JsonOf<HolidayPeriod[]>>('/holiday-period')
    .then((res) => Success.of(res.data.map(deserializeHolidayPeriod)))
    .catch((e) => Failure.fromError(e))
}

export function getHolidayPeriod(id: UUID): Promise<Result<HolidayPeriod>> {
  return client
    .get<JsonOf<HolidayPeriod>>(`/holiday-period/${id}`)
    .then((res) => Success.of(deserializeHolidayPeriod(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function createHolidayPeriod(
  data: HolidayPeriodBody
): Promise<Result<void>> {
  return client
    .post('/holiday-period', data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function updateHolidayPeriod(
  id: UUID,
  data: HolidayPeriodBody
): Promise<Result<void>> {
  return client
    .put(`/holiday-period/${id}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteHolidayPeriod(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/holiday-period/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getQuestionnaires(): Promise<
  Result<FixedPeriodQuestionnaire[]>
> {
  return client
    .get<JsonOf<FixedPeriodQuestionnaire[]>>('/holiday-period/questionnaire')
    .then((res) => res.data.map(deserializeFixedPeriodQuestionnaire))
    .then((val) => Success.of(val))
    .catch((e) => Failure.fromError(e))
}

export function getQuestionnaire(
  id: UUID
): Promise<Result<FixedPeriodQuestionnaire>> {
  return client
    .get<JsonOf<FixedPeriodQuestionnaire>>(
      `/holiday-period/questionnaire/${id}`
    )
    .then((res) => deserializeFixedPeriodQuestionnaire(res.data))
    .then((val) => Success.of(val))
    .catch((e) => Failure.fromError(e))
}

export function createFixedPeriodQuestionnaire(
  data: FixedPeriodQuestionnaireBody
): Promise<Result<void>> {
  return client
    .post(`/holiday-period/questionnaire`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function updateFixedPeriodQuestionnaire(
  id: UUID,
  data: FixedPeriodQuestionnaireBody
): Promise<Result<void>> {
  return client
    .put(`/holiday-period/questionnaire/${id}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteQuestionnaire(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/holiday-period/questionnaire/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
