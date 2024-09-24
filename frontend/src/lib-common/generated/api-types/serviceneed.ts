// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from '../../date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { JsonOf } from '../../json'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.AcceptServiceApplicationBody
*/
export interface AcceptServiceApplicationBody {
  partWeek: boolean
  shiftCareType: ShiftCareType
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationControllerCitizen.CitizenServiceApplication
*/
export interface CitizenServiceApplication {
  data: ServiceApplication
  permittedActions: Action.Citizen.ServiceApplication[]
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.EmployeeServiceApplication
*/
export interface EmployeeServiceApplication {
  data: ServiceApplication
  permittedActions: Action.ServiceApplication[]
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplication
*/
export interface ServiceApplication {
  additionalInfo: string
  childId: UUID
  childName: string
  currentPlacement: ServiceApplicationPlacement | null
  decision: ServiceApplicationDecision | null
  id: UUID
  personId: UUID
  personName: string
  sentAt: HelsinkiDateTime
  serviceNeedOption: ServiceNeedOptionBasics
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationControllerCitizen.ServiceApplicationCreateRequest
*/
export interface ServiceApplicationCreateRequest {
  additionalInfo: string
  childId: UUID
  serviceNeedOptionId: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationDecision
*/
export interface ServiceApplicationDecision {
  decidedAt: HelsinkiDateTime
  decidedBy: UUID
  decidedByName: string
  rejectedReason: string | null
  status: ServiceApplicationDecisionStatus
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationDecisionStatus
*/
export type ServiceApplicationDecisionStatus =
  | 'ACCEPTED'
  | 'REJECTED'

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationPlacement
*/
export interface ServiceApplicationPlacement {
  endDate: LocalDate
  id: UUID
  type: PlacementType
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceApplicationController.ServiceApplicationRejection
*/
export interface ServiceApplicationRejection {
  reason: string
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeed
*/
export interface ServiceNeed {
  confirmed: ServiceNeedConfirmation | null
  endDate: LocalDate
  id: UUID
  option: ServiceNeedOptionSummary
  partWeek: boolean
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
  partWeek: boolean
  placementId: UUID
  shiftCare: ShiftCareType
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOption
*/
export interface ServiceNeedOption {
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
  partWeek: boolean | null
  realizedOccupancyCoefficient: number
  realizedOccupancyCoefficientUnder3y: number
  updated: HelsinkiDateTime
  validFrom: LocalDate
  validPlacementType: PlacementType
  validTo: LocalDate | null
  voucherValueDescriptionFi: string
  voucherValueDescriptionSv: string
}

/**
* Generated from fi.espoo.evaka.serviceneed.application.ServiceNeedOptionBasics
*/
export interface ServiceNeedOptionBasics {
  id: UUID
  nameEn: string
  nameFi: string
  nameSv: string
  partWeek: boolean | null
  validPlacementType: PlacementType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOptionPublicInfo
*/
export interface ServiceNeedOptionPublicInfo {
  id: UUID
  nameEn: string
  nameFi: string
  nameSv: string
  validFrom: LocalDate
  validPlacementType: PlacementType
  validTo: LocalDate | null
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
  partWeek: boolean
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

/**
* Generated from fi.espoo.evaka.serviceneed.application.UndecidedServiceApplicationSummary
*/
export interface UndecidedServiceApplicationSummary {
  childId: UUID
  childName: string
  currentNeed: string | null
  id: UUID
  newNeed: string | null
  placementEndDate: LocalDate
  sentAt: HelsinkiDateTime
  startDate: LocalDate
}


export function deserializeJsonCitizenServiceApplication(json: JsonOf<CitizenServiceApplication>): CitizenServiceApplication {
  return {
    ...json,
    data: deserializeJsonServiceApplication(json.data)
  }
}


export function deserializeJsonEmployeeServiceApplication(json: JsonOf<EmployeeServiceApplication>): EmployeeServiceApplication {
  return {
    ...json,
    data: deserializeJsonServiceApplication(json.data)
  }
}


export function deserializeJsonServiceApplication(json: JsonOf<ServiceApplication>): ServiceApplication {
  return {
    ...json,
    currentPlacement: (json.currentPlacement != null) ? deserializeJsonServiceApplicationPlacement(json.currentPlacement) : null,
    decision: (json.decision != null) ? deserializeJsonServiceApplicationDecision(json.decision) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt),
    serviceNeedOption: deserializeJsonServiceNeedOptionBasics(json.serviceNeedOption),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonServiceApplicationCreateRequest(json: JsonOf<ServiceApplicationCreateRequest>): ServiceApplicationCreateRequest {
  return {
    ...json,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonServiceApplicationDecision(json: JsonOf<ServiceApplicationDecision>): ServiceApplicationDecision {
  return {
    ...json,
    decidedAt: HelsinkiDateTime.parseIso(json.decidedAt)
  }
}


export function deserializeJsonServiceApplicationPlacement(json: JsonOf<ServiceApplicationPlacement>): ServiceApplicationPlacement {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate)
  }
}


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
    updated: HelsinkiDateTime.parseIso(json.updated),
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonServiceNeedOptionBasics(json: JsonOf<ServiceNeedOptionBasics>): ServiceNeedOptionBasics {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonServiceNeedOptionPublicInfo(json: JsonOf<ServiceNeedOptionPublicInfo>): ServiceNeedOptionPublicInfo {
  return {
    ...json,
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
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
    option: (json.option != null) ? deserializeJsonServiceNeedOptionPublicInfo(json.option) : null,
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


export function deserializeJsonUndecidedServiceApplicationSummary(json: JsonOf<UndecidedServiceApplicationSummary>): UndecidedServiceApplicationSummary {
  return {
    ...json,
    placementEndDate: LocalDate.parseIso(json.placementEndDate),
    sentAt: HelsinkiDateTime.parseIso(json.sentAt),
    startDate: LocalDate.parseIso(json.startDate)
  }
}
