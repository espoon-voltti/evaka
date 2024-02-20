// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from '../../date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { ApplicationType } from './application'
import { CreateSource } from './pis'
import { FeeAlterationType } from './invoicing'
import { FeeDecisionStatus } from './invoicing'
import { IncomeEffect } from './invoicing'
import { JsonOf } from '../../json'
import { ModifySource } from './pis'
import { PlacementType } from './placement'
import { UUID } from '../../types'
import { VoucherValueDecisionStatus } from './invoicing'

/**
* Generated from fi.espoo.evaka.timeline.Timeline
*/
export interface Timeline {
  children: TimelineChildDetailed[]
  feeDecisions: TimelineFeeDecision[]
  firstName: string
  incomes: TimelineIncome[]
  lastName: string
  partners: TimelinePartnerDetailed[]
  personId: UUID
  valueDecisions: TimelineValueDecision[]
}

/**
* Generated from fi.espoo.evaka.timeline.TimelineChildDetailed
*/
export interface TimelineChildDetailed {
  childId: UUID
  dateOfBirth: LocalDate
  feeAlterations: TimelineFeeAlteration[]
  firstName: string
  id: UUID
  incomes: TimelineIncome[]
  lastName: string
  placements: TimelinePlacement[]
  range: DateRange
  serviceNeeds: TimelineServiceNeed[]
}

/**
* Generated from fi.espoo.evaka.timeline.TimelineFeeAlteration
*/
export interface TimelineFeeAlteration {
  absolute: boolean
  amount: number
  id: UUID
  notes: string
  range: DateRange
  type: FeeAlterationType
}

/**
* Generated from fi.espoo.evaka.timeline.TimelineFeeDecision
*/
export interface TimelineFeeDecision {
  id: UUID
  range: DateRange
  status: FeeDecisionStatus
  totalFee: number
}

/**
* Generated from fi.espoo.evaka.timeline.TimelineIncome
*/
export interface TimelineIncome {
  effect: IncomeEffect
  id: UUID
  range: DateRange
}

/**
* Generated from fi.espoo.evaka.timeline.TimelinePartnerDetailed
*/
export interface TimelinePartnerDetailed {
  children: TimelineChildDetailed[]
  createSource: CreateSource | null
  createdAt: HelsinkiDateTime | null
  createdBy: UUID | null
  createdByName: string | null
  createdFromApplication: UUID | null
  createdFromApplicationCreated: HelsinkiDateTime | null
  createdFromApplicationType: ApplicationType | null
  feeDecisions: TimelineFeeDecision[]
  firstName: string
  id: UUID
  incomes: TimelineIncome[]
  lastName: string
  modifiedAt: HelsinkiDateTime | null
  modifiedBy: UUID | null
  modifiedByName: string | null
  modifySource: ModifySource | null
  originApplicationAccessible: boolean
  partnerId: UUID
  range: DateRange
  valueDecisions: TimelineValueDecision[]
}

/**
* Generated from fi.espoo.evaka.timeline.TimelinePlacement
*/
export interface TimelinePlacement {
  id: UUID
  range: DateRange
  type: PlacementType
  unit: TimelinePlacementUnit
}

/**
* Generated from fi.espoo.evaka.timeline.TimelinePlacementUnit
*/
export interface TimelinePlacementUnit {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.timeline.TimelineServiceNeed
*/
export interface TimelineServiceNeed {
  id: UUID
  name: string
  range: DateRange
}

/**
* Generated from fi.espoo.evaka.timeline.TimelineValueDecision
*/
export interface TimelineValueDecision {
  id: UUID
  range: DateRange
  status: VoucherValueDecisionStatus
}


export function deserializeJsonTimeline(json: JsonOf<Timeline>): Timeline {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonTimelineChildDetailed(e)),
    feeDecisions: json.feeDecisions.map(e => deserializeJsonTimelineFeeDecision(e)),
    incomes: json.incomes.map(e => deserializeJsonTimelineIncome(e)),
    partners: json.partners.map(e => deserializeJsonTimelinePartnerDetailed(e)),
    valueDecisions: json.valueDecisions.map(e => deserializeJsonTimelineValueDecision(e))
  }
}


export function deserializeJsonTimelineChildDetailed(json: JsonOf<TimelineChildDetailed>): TimelineChildDetailed {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    feeAlterations: json.feeAlterations.map(e => deserializeJsonTimelineFeeAlteration(e)),
    incomes: json.incomes.map(e => deserializeJsonTimelineIncome(e)),
    placements: json.placements.map(e => deserializeJsonTimelinePlacement(e)),
    range: DateRange.parseJson(json.range),
    serviceNeeds: json.serviceNeeds.map(e => deserializeJsonTimelineServiceNeed(e))
  }
}


export function deserializeJsonTimelineFeeAlteration(json: JsonOf<TimelineFeeAlteration>): TimelineFeeAlteration {
  return {
    ...json,
    range: DateRange.parseJson(json.range)
  }
}


export function deserializeJsonTimelineFeeDecision(json: JsonOf<TimelineFeeDecision>): TimelineFeeDecision {
  return {
    ...json,
    range: DateRange.parseJson(json.range)
  }
}


export function deserializeJsonTimelineIncome(json: JsonOf<TimelineIncome>): TimelineIncome {
  return {
    ...json,
    range: DateRange.parseJson(json.range)
  }
}


export function deserializeJsonTimelinePartnerDetailed(json: JsonOf<TimelinePartnerDetailed>): TimelinePartnerDetailed {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonTimelineChildDetailed(e)),
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null,
    createdFromApplicationCreated: (json.createdFromApplicationCreated != null) ? HelsinkiDateTime.parseIso(json.createdFromApplicationCreated) : null,
    feeDecisions: json.feeDecisions.map(e => deserializeJsonTimelineFeeDecision(e)),
    incomes: json.incomes.map(e => deserializeJsonTimelineIncome(e)),
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null,
    range: DateRange.parseJson(json.range),
    valueDecisions: json.valueDecisions.map(e => deserializeJsonTimelineValueDecision(e))
  }
}


export function deserializeJsonTimelinePlacement(json: JsonOf<TimelinePlacement>): TimelinePlacement {
  return {
    ...json,
    range: DateRange.parseJson(json.range)
  }
}


export function deserializeJsonTimelineServiceNeed(json: JsonOf<TimelineServiceNeed>): TimelineServiceNeed {
  return {
    ...json,
    range: DateRange.parseJson(json.range)
  }
}


export function deserializeJsonTimelineValueDecision(json: JsonOf<TimelineValueDecision>): TimelineValueDecision {
  return {
    ...json,
    range: DateRange.parseJson(json.range)
  }
}
