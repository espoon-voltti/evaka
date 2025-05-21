// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { AssistanceNeedDecisionGuardianId } from './shared'
import type { AssistanceNeedDecisionId } from './shared'
import type { AssistanceNeedPreschoolDecisionGuardianId } from './shared'
import type { AssistanceNeedPreschoolDecisionId } from './shared'
import type { AssistanceNeedVoucherCoefficientId } from './shared'
import DateRange from '../../date-range'
import type { DaycareId } from './shared'
import type { EmployeeId } from './shared'
import type { EvakaUser } from './user'
import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { OfficialLanguage } from './shared'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.AnnulAssistanceNeedDecisionRequest
*/
export interface AnnulAssistanceNeedDecisionRequest {
  reason: string
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.AnnulAssistanceNeedPreschoolDecisionRequest
*/
export interface AnnulAssistanceNeedPreschoolDecisionRequest {
  reason: string
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
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecision
*/
export interface AssistanceNeedDecision {
  annulmentReason: string
  assistanceLevels: AssistanceLevel[]
  careMotivation: string | null
  child: AssistanceNeedDecisionChild | null
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionMaker | null
  decisionNumber: number | null
  endDateNotKnown: boolean
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  hasDocument: boolean
  id: AssistanceNeedDecisionId
  language: OfficialLanguage
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
  status: AssistanceNeedDecisionStatus
  structuralMotivationDescription: string | null
  structuralMotivationOptions: StructuralMotivationOptions
  validityPeriod: DateRange
  viewOfGuardians: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionBasics
*/
export interface AssistanceNeedDecisionBasics {
  createdAt: HelsinkiDateTime
  decisionMade: LocalDate | null
  id: AssistanceNeedDecisionId
  selectedUnit: UnitInfoBasics | null
  sentForDecision: LocalDate | null
  status: AssistanceNeedDecisionStatus
  validityPeriod: DateRange
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
  dateOfBirth: LocalDate | null
  id: PersonId | null
  name: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionCitizenListItem
*/
export interface AssistanceNeedDecisionCitizenListItem {
  annulmentReason: string
  assistanceLevels: AssistanceLevel[]
  childId: PersonId
  decisionMade: LocalDate
  id: AssistanceNeedDecisionId
  isUnread: boolean
  selectedUnit: UnitInfoBasics | null
  status: AssistanceNeedDecisionStatus
  validityPeriod: DateRange
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployee
*/
export interface AssistanceNeedDecisionEmployee {
  employeeId: EmployeeId | null
  name: string | null
  phoneNumber: string | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployeeForm
*/
export interface AssistanceNeedDecisionEmployeeForm {
  employeeId: EmployeeId | null
  phoneNumber: string | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionForm
*/
export interface AssistanceNeedDecisionForm {
  assistanceLevels: AssistanceLevel[]
  careMotivation: string | null
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionMakerForm | null
  decisionNumber: number | null
  endDateNotKnown: boolean
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  language: OfficialLanguage
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
  status: AssistanceNeedDecisionStatus
  structuralMotivationDescription: string | null
  structuralMotivationOptions: StructuralMotivationOptions
  validityPeriod: DateRange
  viewOfGuardians: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionGuardian
*/
export interface AssistanceNeedDecisionGuardian {
  details: string | null
  id: AssistanceNeedDecisionGuardianId | null
  isHeard: boolean
  name: string
  personId: PersonId | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionMaker
*/
export interface AssistanceNeedDecisionMaker {
  employeeId: EmployeeId | null
  name: string | null
  title: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionMakerForm
*/
export interface AssistanceNeedDecisionMakerForm {
  employeeId: EmployeeId | null
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
  hasMissingFields: boolean
  permittedActions: Action.AssistanceNeedDecision[]
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
*/
export const assistanceNeedDecisionStatuses = [
  'DRAFT',
  'NEEDS_WORK',
  'ACCEPTED',
  'REJECTED',
  'ANNULLED'
] as const

export type AssistanceNeedDecisionStatus = typeof assistanceNeedDecisionStatuses[number]

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecision
*/
export interface AssistanceNeedPreschoolDecision {
  annulmentReason: string
  child: AssistanceNeedPreschoolDecisionChild
  decisionMade: LocalDate | null
  decisionMakerHasOpened: boolean
  decisionMakerName: string | null
  decisionNumber: number
  form: AssistanceNeedPreschoolDecisionForm
  hasDocument: boolean
  id: AssistanceNeedPreschoolDecisionId
  isValid: boolean
  preparer1Name: string | null
  preparer2Name: string | null
  sentForDecision: LocalDate | null
  status: AssistanceNeedDecisionStatus
  unitName: string | null
  unitPostOffice: string | null
  unitPostalCode: string | null
  unitStreetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionBasics
*/
export interface AssistanceNeedPreschoolDecisionBasics {
  annulmentReason: string
  childId: PersonId
  createdAt: HelsinkiDateTime
  decisionMade: LocalDate | null
  id: AssistanceNeedPreschoolDecisionId
  selectedUnit: UnitInfoBasics | null
  sentForDecision: LocalDate | null
  status: AssistanceNeedDecisionStatus
  type: AssistanceNeedPreschoolDecisionType | null
  unreadGuardianIds: PersonId[] | null
  validFrom: LocalDate | null
  validTo: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.AssistanceNeedPreschoolDecisionBasicsResponse
*/
export interface AssistanceNeedPreschoolDecisionBasicsResponse {
  decision: AssistanceNeedPreschoolDecisionBasics
  permittedActions: Action.AssistanceNeedPreschoolDecision[]
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionChild
*/
export interface AssistanceNeedPreschoolDecisionChild {
  dateOfBirth: LocalDate
  id: PersonId
  name: string
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionCitizenListItem
*/
export interface AssistanceNeedPreschoolDecisionCitizenListItem {
  annulmentReason: string
  childId: PersonId
  decisionMade: LocalDate
  id: AssistanceNeedPreschoolDecisionId
  isUnread: boolean
  status: AssistanceNeedDecisionStatus
  type: AssistanceNeedPreschoolDecisionType
  unitName: string
  validityPeriod: DateRange
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionForm
*/
export interface AssistanceNeedPreschoolDecisionForm {
  basisDocumentDoctorStatement: boolean
  basisDocumentDoctorStatementDate: LocalDate | null
  basisDocumentOtherOrMissing: boolean
  basisDocumentOtherOrMissingInfo: string
  basisDocumentPedagogicalReport: boolean
  basisDocumentPedagogicalReportDate: LocalDate | null
  basisDocumentPsychologistStatement: boolean
  basisDocumentPsychologistStatementDate: LocalDate | null
  basisDocumentSocialReport: boolean
  basisDocumentSocialReportDate: LocalDate | null
  basisDocumentsInfo: string
  decisionBasis: string
  decisionMakerEmployeeId: EmployeeId | null
  decisionMakerTitle: string
  extendedCompulsoryEducation: boolean
  extendedCompulsoryEducationInfo: string
  grantedAssistanceService: boolean
  grantedAssistiveDevices: boolean
  grantedInterpretationService: boolean
  grantedServicesBasis: string
  guardianInfo: AssistanceNeedPreschoolDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  language: OfficialLanguage
  otherRepresentativeDetails: string
  otherRepresentativeHeard: boolean
  preparer1EmployeeId: EmployeeId | null
  preparer1PhoneNumber: string
  preparer1Title: string
  preparer2EmployeeId: EmployeeId | null
  preparer2PhoneNumber: string
  preparer2Title: string
  primaryGroup: string
  selectedUnit: DaycareId | null
  type: AssistanceNeedPreschoolDecisionType | null
  validFrom: LocalDate | null
  validTo: LocalDate | null
  viewOfGuardians: string
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionGuardian
*/
export interface AssistanceNeedPreschoolDecisionGuardian {
  details: string
  id: AssistanceNeedPreschoolDecisionGuardianId
  isHeard: boolean
  name: string
  personId: PersonId
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.AssistanceNeedPreschoolDecisionResponse
*/
export interface AssistanceNeedPreschoolDecisionResponse {
  decision: AssistanceNeedPreschoolDecision
  permittedActions: Action.AssistanceNeedPreschoolDecision[]
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionType
*/
export type AssistanceNeedPreschoolDecisionType =
  | 'NEW'
  | 'CONTINUING'
  | 'TERMINATED'

/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficient
*/
export interface AssistanceNeedVoucherCoefficient {
  childId: PersonId
  coefficient: number
  id: AssistanceNeedVoucherCoefficientId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  validityPeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
*/
export interface AssistanceNeedVoucherCoefficientRequest {
  coefficient: number
  validityPeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientResponse
*/
export interface AssistanceNeedVoucherCoefficientResponse {
  permittedActions: Action.AssistanceNeedVoucherCoefficient[]
  voucherCoefficient: AssistanceNeedVoucherCoefficient
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.DecideAssistanceNeedDecisionRequest
*/
export interface DecideAssistanceNeedDecisionRequest {
  status: AssistanceNeedDecisionStatus
}

/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.DecideAssistanceNeedPreschoolDecisionRequest
*/
export interface DecideAssistanceNeedPreschoolDecisionRequest {
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
  id: DaycareId | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.UnitInfo
*/
export interface UnitInfo {
  id: DaycareId | null
  name: string | null
  postOffice: string | null
  postalCode: string | null
  streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.UnitInfoBasics
*/
export interface UnitInfoBasics {
  id: DaycareId | null
  name: string | null
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.UnreadAssistanceNeedDecisionItem
*/
export interface UnreadAssistanceNeedDecisionItem {
  childId: PersonId
  count: number
}

/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.UpdateDecisionMakerForAssistanceNeedDecisionRequest
*/
export interface UpdateDecisionMakerForAssistanceNeedDecisionRequest {
  title: string
}


export function deserializeJsonAssistanceNeedDecision(json: JsonOf<AssistanceNeedDecision>): AssistanceNeedDecision {
  return {
    ...json,
    child: (json.child != null) ? deserializeJsonAssistanceNeedDecisionChild(json.child) : null,
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    guardiansHeardOn: (json.guardiansHeardOn != null) ? LocalDate.parseIso(json.guardiansHeardOn) : null,
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null,
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedDecisionBasics(json: JsonOf<AssistanceNeedDecisionBasics>): AssistanceNeedDecisionBasics {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null,
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedDecisionBasicsResponse(json: JsonOf<AssistanceNeedDecisionBasicsResponse>): AssistanceNeedDecisionBasicsResponse {
  return {
    ...json,
    decision: deserializeJsonAssistanceNeedDecisionBasics(json.decision)
  }
}


export function deserializeJsonAssistanceNeedDecisionChild(json: JsonOf<AssistanceNeedDecisionChild>): AssistanceNeedDecisionChild {
  return {
    ...json,
    dateOfBirth: (json.dateOfBirth != null) ? LocalDate.parseIso(json.dateOfBirth) : null
  }
}


export function deserializeJsonAssistanceNeedDecisionCitizenListItem(json: JsonOf<AssistanceNeedDecisionCitizenListItem>): AssistanceNeedDecisionCitizenListItem {
  return {
    ...json,
    decisionMade: LocalDate.parseIso(json.decisionMade),
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedDecisionForm(json: JsonOf<AssistanceNeedDecisionForm>): AssistanceNeedDecisionForm {
  return {
    ...json,
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    guardiansHeardOn: (json.guardiansHeardOn != null) ? LocalDate.parseIso(json.guardiansHeardOn) : null,
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null,
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedDecisionRequest(json: JsonOf<AssistanceNeedDecisionRequest>): AssistanceNeedDecisionRequest {
  return {
    ...json,
    decision: deserializeJsonAssistanceNeedDecisionForm(json.decision)
  }
}


export function deserializeJsonAssistanceNeedDecisionResponse(json: JsonOf<AssistanceNeedDecisionResponse>): AssistanceNeedDecisionResponse {
  return {
    ...json,
    decision: deserializeJsonAssistanceNeedDecision(json.decision)
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecision(json: JsonOf<AssistanceNeedPreschoolDecision>): AssistanceNeedPreschoolDecision {
  return {
    ...json,
    child: deserializeJsonAssistanceNeedPreschoolDecisionChild(json.child),
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    form: deserializeJsonAssistanceNeedPreschoolDecisionForm(json.form),
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecisionBasics(json: JsonOf<AssistanceNeedPreschoolDecisionBasics>): AssistanceNeedPreschoolDecisionBasics {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null,
    validFrom: (json.validFrom != null) ? LocalDate.parseIso(json.validFrom) : null,
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecisionBasicsResponse(json: JsonOf<AssistanceNeedPreschoolDecisionBasicsResponse>): AssistanceNeedPreschoolDecisionBasicsResponse {
  return {
    ...json,
    decision: deserializeJsonAssistanceNeedPreschoolDecisionBasics(json.decision)
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecisionChild(json: JsonOf<AssistanceNeedPreschoolDecisionChild>): AssistanceNeedPreschoolDecisionChild {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecisionCitizenListItem(json: JsonOf<AssistanceNeedPreschoolDecisionCitizenListItem>): AssistanceNeedPreschoolDecisionCitizenListItem {
  return {
    ...json,
    decisionMade: LocalDate.parseIso(json.decisionMade),
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecisionForm(json: JsonOf<AssistanceNeedPreschoolDecisionForm>): AssistanceNeedPreschoolDecisionForm {
  return {
    ...json,
    basisDocumentDoctorStatementDate: (json.basisDocumentDoctorStatementDate != null) ? LocalDate.parseIso(json.basisDocumentDoctorStatementDate) : null,
    basisDocumentPedagogicalReportDate: (json.basisDocumentPedagogicalReportDate != null) ? LocalDate.parseIso(json.basisDocumentPedagogicalReportDate) : null,
    basisDocumentPsychologistStatementDate: (json.basisDocumentPsychologistStatementDate != null) ? LocalDate.parseIso(json.basisDocumentPsychologistStatementDate) : null,
    basisDocumentSocialReportDate: (json.basisDocumentSocialReportDate != null) ? LocalDate.parseIso(json.basisDocumentSocialReportDate) : null,
    guardiansHeardOn: (json.guardiansHeardOn != null) ? LocalDate.parseIso(json.guardiansHeardOn) : null,
    validFrom: (json.validFrom != null) ? LocalDate.parseIso(json.validFrom) : null,
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonAssistanceNeedPreschoolDecisionResponse(json: JsonOf<AssistanceNeedPreschoolDecisionResponse>): AssistanceNeedPreschoolDecisionResponse {
  return {
    ...json,
    decision: deserializeJsonAssistanceNeedPreschoolDecision(json.decision)
  }
}


export function deserializeJsonAssistanceNeedVoucherCoefficient(json: JsonOf<AssistanceNeedVoucherCoefficient>): AssistanceNeedVoucherCoefficient {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validityPeriod: FiniteDateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedVoucherCoefficientRequest(json: JsonOf<AssistanceNeedVoucherCoefficientRequest>): AssistanceNeedVoucherCoefficientRequest {
  return {
    ...json,
    validityPeriod: FiniteDateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedVoucherCoefficientResponse(json: JsonOf<AssistanceNeedVoucherCoefficientResponse>): AssistanceNeedVoucherCoefficientResponse {
  return {
    ...json,
    voucherCoefficient: deserializeJsonAssistanceNeedVoucherCoefficient(json.voucherCoefficient)
  }
}
