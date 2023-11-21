// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  Timeline,
  TimelineChildDetailed,
  TimelineFeeAlteration,
  TimelineFeeDecision,
  TimelineIncome,
  TimelinePartnerDetailed,
  TimelinePlacement,
  TimelineServiceNeed,
  TimelineValueDecision
} from 'lib-common/generated/api-types/timeline'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../api/client'

export function getTimeline(
  personId: UUID,
  searchRange: FiniteDateRange
): Promise<Timeline> {
  return client
    .get<JsonOf<Timeline>>('/timeline', {
      params: {
        personId,
        from: searchRange.start.formatIso(),
        to: searchRange.end.formatIso()
      }
    })
    .then((res) => deserializeTimeline(res.data))
}

const deserializeTimeline = (json: JsonOf<Timeline>): Timeline => ({
  ...json,
  feeDecisions: json.feeDecisions.map(deserializeFeeDecision),
  valueDecisions: json.valueDecisions.map(deserializeValueDecision),
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

const deserializeValueDecision = (
  json: JsonOf<TimelineValueDecision>
): TimelineValueDecision => ({
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
  createdAt: json.createdAt ? HelsinkiDateTime.parseIso(json.createdAt) : null,
  modifiedAt: json.modifiedAt
    ? HelsinkiDateTime.parseIso(json.modifiedAt)
    : null,
  createdFromApplicationCreated: json.createdFromApplicationCreated
    ? HelsinkiDateTime.parseIso(json.createdFromApplicationCreated)
    : null,
  range: DateRange.parseJson(json.range),
  feeDecisions: json.feeDecisions.map(deserializeFeeDecision),
  valueDecisions: json.valueDecisions.map(deserializeValueDecision),
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
  serviceNeeds: json.serviceNeeds.map(deserializeServiceNeed),
  feeAlterations: json.feeAlterations.map(deserializeFeeAlteration)
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

const deserializeFeeAlteration = (
  json: JsonOf<TimelineFeeAlteration>
): TimelineFeeAlteration => ({
  ...json,
  range: DateRange.parseJson(json.range)
})
