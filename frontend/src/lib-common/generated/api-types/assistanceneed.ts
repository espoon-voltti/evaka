// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceBasisOption
*/
export interface AssistanceBasisOption {
  descriptionFi: string | null
  nameFi: string
  value: string
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceLevel
*/
export type AssistanceLevel =
  | 'ASSISTANCE_ENDS'
  | 'ASSISTANCE_SERVICES_FOR_TIME'
  | 'ENHANCED_ASSISTANCE'
  | 'SPECIAL_ASSISTANCE'

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeed
*/
export interface AssistanceNeed {
  bases: string[]
  capacityFactor: number
  childId: UUID
  endDate: LocalDate
  id: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecision
*/
export interface AssistanceNeedDecision {
  assistanceLevel: AssistanceLevel | null
  assistanceServicesTime: FiniteDateRange | null
  careMotivation: string | null
  childId: UUID
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionEmployee | null
  decisionNumber: number | null
  endDate: LocalDate | null
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  id: UUID | null
  language: AssistanceNeedDecisionLanguage
  motivationForDecision: string | null
  otherRepresentativeDetails: string | null
  otherRepresentativeHeard: boolean
  pedagogicalMotivation: string | null
  preparedBy1: AssistanceNeedDecisionEmployee | null
  preparedBy2: AssistanceNeedDecisionEmployee | null
  selectedUnit: UnitInfo | null
  sentForDecision: LocalDate | null
  serviceOptions: ServiceOptions
  servicesMotivation: string | null
  startDate: LocalDate | null
  status: AssistanceNeedDecisionStatus
  structuralMotivationDescription: string | null
  structuralMotivationOptions: StructuralMotivationOptions
  viewOfGuardians: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployee
*/
export interface AssistanceNeedDecisionEmployee {
  email: string | null
  employeeId: UUID | null
  name: string | null
  phoneNumber: string | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionGuardian
*/
export interface AssistanceNeedDecisionGuardian {
  details: string | null
  id: UUID | null
  isHeard: boolean
  name: string
  personId: UUID | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionLanguage
*/
export type AssistanceNeedDecisionLanguage =
  | 'FI'
  | 'SV'

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionRequest
*/
export interface AssistanceNeedDecisionRequest {
  decision: AssistanceNeedDecision
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
*/
export type AssistanceNeedDecisionStatus =
  | 'DRAFT'
  | 'NEEDS_WORK'
  | 'ACCEPTED'
  | 'REJECTED'

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeedRequest
*/
export interface AssistanceNeedRequest {
  bases: string[]
  capacityFactor: number
  endDate: LocalDate
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeedResponse
*/
export interface AssistanceNeedResponse {
  need: AssistanceNeed
  permittedActions: Action.AssistanceNeed[]
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.ServiceOptions
*/
export interface ServiceOptions {
  consultationSpecialEd: boolean
  fullTimeSpecialEd: boolean
  interpretationAndAssistanceServices: boolean
  partTimeSpecialEd: boolean
  specialAides: boolean
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
*/
export interface StructuralMotivationOptions {
  additionalStaff: boolean
  childAssistant: boolean
  groupAssistant: boolean
  smallGroup: boolean
  smallerGroup: boolean
  specialGroup: boolean
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.UnitInfo
*/
export interface UnitInfo {
  id: UUID | null
  name: string | null
  postOffice: string | null
  postalCode: string | null
  streetAddress: string | null
}
