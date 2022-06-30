// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
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
  child: AssistanceNeedDecisionChild | null
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionMaker | null
  decisionNumber: number | null
  endDate: LocalDate | null
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  id: UUID
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
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionBasics
*/
export interface AssistanceNeedDecisionBasics {
  created: HelsinkiDateTime
  decisionMade: LocalDate | null
  endDate: LocalDate | null
  id: UUID
  selectedUnit: UnitInfoBasics | null
  sentForDecision: LocalDate | null
  startDate: LocalDate | null
  status: AssistanceNeedDecisionStatus
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.AssistanceNeedDecisionBasicsResponse
*/
export interface AssistanceNeedDecisionBasicsResponse {
  decision: AssistanceNeedDecisionBasics
  permittedActions: Action.AssistanceNeedDecision[]
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionChild
*/
export interface AssistanceNeedDecisionChild {
  id: UUID | null
  name: string | null
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
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployeeForm
*/
export interface AssistanceNeedDecisionEmployeeForm {
  employeeId: UUID | null
  phoneNumber: string | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionForm
*/
export interface AssistanceNeedDecisionForm {
  assistanceLevel: AssistanceLevel | null
  assistanceServicesTime: FiniteDateRange | null
  careMotivation: string | null
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionMakerForm | null
  decisionNumber: number | null
  endDate: LocalDate | null
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  language: AssistanceNeedDecisionLanguage
  motivationForDecision: string | null
  otherRepresentativeDetails: string | null
  otherRepresentativeHeard: boolean
  pedagogicalMotivation: string | null
  preparedBy1: AssistanceNeedDecisionEmployeeForm | null
  preparedBy2: AssistanceNeedDecisionEmployeeForm | null
  selectedUnit: UnitIdInfo | null
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
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionMaker
*/
export interface AssistanceNeedDecisionMaker {
  employeeId: UUID | null
  name: string | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionMakerForm
*/
export interface AssistanceNeedDecisionMakerForm {
  employeeId: UUID | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionRequest
*/
export interface AssistanceNeedDecisionRequest {
  decision: AssistanceNeedDecisionForm
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.AssistanceNeedDecisionResponse
*/
export interface AssistanceNeedDecisionResponse {
  decision: AssistanceNeedDecision
  permittedActions: Action.AssistanceNeedDecision[]
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
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest
*/
export interface DecideAssistanceNeedDecisionRequest {
  status: AssistanceNeedDecisionStatus
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
* Generated from fi.espoo.evaka.assistanceneed.decision.UnitIdInfo
*/
export interface UnitIdInfo {
  id: UUID | null
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

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.UnitInfoBasics
*/
export interface UnitInfoBasics {
  id: UUID | null
  name: string | null
}
