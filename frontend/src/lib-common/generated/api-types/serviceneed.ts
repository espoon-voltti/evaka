// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { JsonOf } from '../../json'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeed
*/
export interface ServiceNeed {
  confirmed: ServiceNeedConfirmation | null
  endDate: LocalDate
  id: UUID
  option: ServiceNeedOptionSummary
  placementId: UUID
  shiftCare: ShiftCareType
  startDate: LocalDate
  updated: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedConfirmation
*/
export interface ServiceNeedConfirmation {
  at: HelsinkiDateTime | null
  name: string
  userId: UUID
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.ServiceNeedCreateRequest
*/
export interface ServiceNeedCreateRequest {
  endDate: LocalDate
  optionId: UUID
  placementId: UUID
  shiftCare: ShiftCareType
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOption
*/
export interface ServiceNeedOption {
  active: boolean
  contractDaysPerMonth: number | null
  daycareHoursPerMonth: number | null
  daycareHoursPerWeek: number
  defaultOption: boolean
  feeCoefficient: number
  feeDescriptionFi: string
  feeDescriptionSv: string
  id: UUID
  nameEn: string
  nameFi: string
  nameSv: string
  occupancyCoefficient: number
  occupancyCoefficientUnder3y: number
  partDay: boolean
  partWeek: boolean
  realizedOccupancyCoefficient: number
  realizedOccupancyCoefficientUnder3y: number
  updated: HelsinkiDateTime
  validPlacementType: PlacementType
  voucherValueDescriptionFi: string
  voucherValueDescriptionSv: string
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOptionPublicInfo
*/
export interface ServiceNeedOptionPublicInfo {
  id: UUID
  nameEn: string
  nameFi: string
  nameSv: string
  validPlacementType: PlacementType
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOptionSummary
*/
export interface ServiceNeedOptionSummary {
  id: UUID
  nameEn: string
  nameFi: string
  nameSv: string
  updated: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedSummary
*/
export interface ServiceNeedSummary {
  contractDaysPerMonth: number | null
  endDate: LocalDate
  option: ServiceNeedOptionPublicInfo | null
  startDate: LocalDate
  unitName: string
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.ServiceNeedUpdateRequest
*/
export interface ServiceNeedUpdateRequest {
  endDate: LocalDate
  optionId: UUID
  shiftCare: ShiftCareType
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.ShiftCareType
*/
export const shiftCareType = [
  'NONE',
  'INTERMITTENT',
  'FULL'
] as const

export type ShiftCareType = typeof shiftCareType[number]


export function deserializeJsonServiceNeed(json: JsonOf<ServiceNeed>): ServiceNeed {
  return {
    ...json,
    confirmed: (json.confirmed != null) ? deserializeJsonServiceNeedConfirmation(json.confirmed) : null,
    endDate: LocalDate.parseIso(json.endDate),
    option: deserializeJsonServiceNeedOptionSummary(json.option),
    startDate: LocalDate.parseIso(json.startDate),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonServiceNeedConfirmation(json: JsonOf<ServiceNeedConfirmation>): ServiceNeedConfirmation {
  return {
    ...json,
    at: (json.at != null) ? HelsinkiDateTime.parseIso(json.at) : null
  }
}


export function deserializeJsonServiceNeedCreateRequest(json: JsonOf<ServiceNeedCreateRequest>): ServiceNeedCreateRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonServiceNeedOption(json: JsonOf<ServiceNeedOption>): ServiceNeedOption {
  return {
    ...json,
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonServiceNeedOptionSummary(json: JsonOf<ServiceNeedOptionSummary>): ServiceNeedOptionSummary {
  return {
    ...json,
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonServiceNeedSummary(json: JsonOf<ServiceNeedSummary>): ServiceNeedSummary {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonServiceNeedUpdateRequest(json: JsonOf<ServiceNeedUpdateRequest>): ServiceNeedUpdateRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}
