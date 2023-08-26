// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import {
  Timeline,
  TimelineChildDetailed,
  TimelineFeeDecision,
  TimelineIncome,
  TimelinePartnerDetailed,
  TimelinePlacement,
  TimelineServiceNeed
} from 'lib-common/generated/api-types/timeline'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from './client'

export function getTimeline(personId: UUID): Promise<Result<Timeline>> {
  return client
    .get<JsonOf<Timeline>>('/timeline', { params: { personId } })
    .then((res) => Success.of(deserializeTimeline(res.data)))
    .catch((e) => Failure.fromError(e))
}

const deserializeTimeline = (json: JsonOf<Timeline>): Timeline => ({
  ...json,
  feeDecisions: json.feeDecisions.map(deserializeFeeDecision),
  incomes: json.incomes.map(deserializeIncome),
  partners: json.partners.map(deserializePartner),
  children: json.children.map(deserializeChild)
})

const deserializeFeeDecision = (
  json: JsonOf<TimelineFeeDecision>
): TimelineFeeDecision => ({
  ...json,
  range: DateRange.parseJson(json.range)
})

const deserializeIncome = (json: JsonOf<TimelineIncome>): TimelineIncome => ({
  ...json,
  range: DateRange.parseJson(json.range)
})

const deserializePartner = (
  json: JsonOf<TimelinePartnerDetailed>
): TimelinePartnerDetailed => ({
  ...json,
  range: DateRange.parseJson(json.range),
  feeDecisions: json.feeDecisions.map(deserializeFeeDecision),
  incomes: json.incomes.map(deserializeIncome),
  children: json.children.map(deserializeChild)
})

const deserializeChild = (
  json: JsonOf<TimelineChildDetailed>
): TimelineChildDetailed => ({
  ...json,
  range: DateRange.parseJson(json.range),
  dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
  incomes: json.incomes.map(deserializeIncome),
  placements: json.placements.map(deserializePlacement),
  serviceNeeds: json.serviceNeeds.map(deserializeServiceNeed)
})

const deserializePlacement = (
  json: JsonOf<TimelinePlacement>
): TimelinePlacement => ({
  ...json,
  range: DateRange.parseJson(json.range)
})

const deserializeServiceNeed = (
  json: JsonOf<TimelineServiceNeed>
): TimelineServiceNeed => ({
  ...json,
  range: DateRange.parseJson(json.range)
})
