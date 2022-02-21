// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import DateRange from '../../date-range'
import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { Coordinate } from './shared'
import { DaycareAclRow } from './shared'
import { EvakaUserType } from './user'
import { PersonJSON } from './pis'
import { PilotFeature } from './shared'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.daycare.service.Absence
*/
export interface Absence {
  absenceType: AbsenceType
  category: AbsenceCategory
  childId: UUID
  date: LocalDate
  id: UUID
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceCategory
*/
export type AbsenceCategory = 
  | 'BILLABLE'
  | 'NONBILLABLE'

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceChild
*/
export interface AbsenceChild {
  absences: Record<string, AbsenceWithModifierInfo[]>
  attendanceTotalHours: number | null
  backupCares: Record<string, boolean>
  child: Child
  placements: Record<string, AbsenceCategory[]>
  reservationTotalHours: number | null
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceDelete
*/
export interface AbsenceDelete {
  category: AbsenceCategory
  childId: UUID
  date: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceGroup
*/
export interface AbsenceGroup {
  children: AbsenceChild[]
  daycareName: string
  groupId: UUID
  groupName: string
  operationDays: LocalDate[]
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceType
*/
export type AbsenceType = 
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'UNKNOWN_ABSENCE'
  | 'PLANNED_ABSENCE'
  | 'PARENTLEAVE'
  | 'FORCE_MAJEURE'
  | 'FREE_ABSENCE'

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceUpsert
*/
export interface AbsenceUpsert {
  absenceType: AbsenceType
  category: AbsenceCategory
  childId: UUID
  date: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceWithModifierInfo
*/
export interface AbsenceWithModifierInfo {
  absenceType: AbsenceType
  category: AbsenceCategory
  childId: UUID
  date: LocalDate
  id: UUID
  modifiedAt: Date
  modifiedByType: EvakaUserType
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.AdditionalInformation
*/
export interface AdditionalInformation {
  additionalInfo: string
  allergies: string
  diet: string
  medication: string
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.AreaJSON
*/
export interface AreaJSON {
  id: UUID
  name: string
  shortName: string
}

/**
* Generated from fi.espoo.evaka.daycare.CareType
*/
export type CareType = 
  | 'CLUB'
  | 'FAMILY'
  | 'CENTRE'
  | 'GROUP_FAMILY'
  | 'PRESCHOOL'
  | 'PREPARATORY_EDUCATION'

/**
* Generated from fi.espoo.evaka.daycare.CaretakerAmount
*/
export interface CaretakerAmount {
  amount: number
  endDate: LocalDate | null
  groupId: UUID
  id: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.CaretakerRequest
*/
export interface CaretakerRequest {
  amount: number
  endDate: LocalDate | null
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.CaretakersResponse
*/
export interface CaretakersResponse {
  caretakers: CaretakerAmount[]
  groupName: string
  unitName: string
}

/**
* Generated from fi.espoo.evaka.daycare.service.Child
*/
export interface Child {
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.ChildController.ChildResponse
*/
export interface ChildResponse {
  permittedActions: Action.Child[]
  permittedPersonActions: Action.Person[]
  person: PersonJSON
}

/**
* Generated from fi.espoo.evaka.daycare.ClubTerm
*/
export interface ClubTerm {
  applicationPeriod: FiniteDateRange
  term: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.CreateDaycareResponse
*/
export interface CreateDaycareResponse {
  id: UUID
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.CreateGroupRequest
*/
export interface CreateGroupRequest {
  initialCaretakers: number
  name: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.Daycare
*/
export interface Daycare {
  additionalInfo: string | null
  area: DaycareCareArea
  capacity: number
  closingDate: LocalDate | null
  clubApplyPeriod: DateRange | null
  costCenter: string | null
  daycareApplyPeriod: DateRange | null
  decisionCustomization: DaycareDecisionCustomization
  email: string | null
  enabledPilotFeatures: PilotFeature[]
  financeDecisionHandler: FinanceDecisionHandler | null
  ghostUnit: boolean
  id: UUID
  invoicedByMunicipality: boolean
  language: Language
  location: Coordinate | null
  mailingAddress: MailingAddress
  name: string
  openingDate: LocalDate | null
  operationDays: number[]
  ophOrganizerOid: string | null
  ophUnitOid: string | null
  phone: string | null
  preschoolApplyPeriod: DateRange | null
  providerType: ProviderType
  roundTheClock: boolean
  type: CareType[]
  unitManager: UnitManager
  uploadChildrenToVarda: boolean
  uploadToKoski: boolean
  uploadToVarda: boolean
  url: string | null
  visitingAddress: VisitingAddress
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareAclResponse
*/
export interface DaycareAclResponse {
  rows: DaycareAclRow[]
}

/**
* Generated from fi.espoo.evaka.daycare.DaycareCareArea
*/
export interface DaycareCareArea {
  id: UUID
  name: string
  shortName: string
}

/**
* Generated from fi.espoo.evaka.daycare.DaycareDecisionCustomization
*/
export interface DaycareDecisionCustomization {
  daycareName: string
  handler: string
  handlerAddress: string
  preschoolName: string
}

/**
* Generated from fi.espoo.evaka.daycare.DaycareFields
*/
export interface DaycareFields {
  additionalInfo: string | null
  areaId: UUID
  capacity: number
  closingDate: LocalDate | null
  clubApplyPeriod: DateRange | null
  costCenter: string | null
  daycareApplyPeriod: DateRange | null
  decisionCustomization: DaycareDecisionCustomization
  email: string | null
  financeDecisionHandlerId: UUID | null
  ghostUnit: boolean
  invoicedByMunicipality: boolean
  language: Language
  location: Coordinate | null
  mailingAddress: MailingAddress
  name: string
  openingDate: LocalDate | null
  operationDays: number[] | null
  ophOrganizerOid: string | null
  ophUnitOid: string | null
  phone: string | null
  preschoolApplyPeriod: DateRange | null
  providerType: ProviderType
  roundTheClock: boolean
  type: CareType[]
  unitManager: UnitManager
  uploadChildrenToVarda: boolean
  uploadToKoski: boolean
  uploadToVarda: boolean
  url: string | null
  visitingAddress: VisitingAddress
}

/**
* Generated from fi.espoo.evaka.daycare.service.DaycareGroup
*/
export interface DaycareGroup {
  daycareId: UUID
  deletable: boolean
  endDate: LocalDate | null
  id: UUID
  name: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.FinanceDecisionHandler
*/
export interface FinanceDecisionHandler {
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.GroupAclUpdate
*/
export interface GroupAclUpdate {
  groupIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.daycare.service.GroupStaffAttendance
*/
export interface GroupStaffAttendance {
  count: number
  countOther: number
  date: LocalDate
  groupId: UUID
  updated: Date
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.GroupUpdateRequest
*/
export interface GroupUpdateRequest {
  endDate: LocalDate | null
  name: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.domain.Language
*/
export type Language = 
  | 'fi'
  | 'sv'
  | 'en'

/**
* Generated from fi.espoo.evaka.daycare.MailingAddress
*/
export interface MailingAddress {
  poBox: string | null
  postOffice: string | null
  postalCode: string | null
  streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.daycare.PreschoolTerm
*/
export interface PreschoolTerm {
  applicationPeriod: FiniteDateRange
  extendedTerm: FiniteDateRange
  finnishPreschool: FiniteDateRange
  swedishPreschool: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.daycare.domain.ProviderType
*/
export type ProviderType = 
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'
  | 'EXTERNAL_PURCHASED'

/**
* Generated from fi.espoo.evaka.daycare.controllers.PublicUnit
*/
export interface PublicUnit {
  clubApplyPeriod: DateRange | null
  daycareApplyPeriod: DateRange | null
  email: string | null
  ghostUnit: boolean | null
  id: UUID
  language: Language
  location: Coordinate | null
  name: string
  phone: string | null
  postOffice: string
  postalCode: string
  preschoolApplyPeriod: DateRange | null
  providerType: ProviderType
  roundTheClock: boolean
  streetAddress: string
  type: CareType[]
  url: string | null
}

/**
* Generated from fi.espoo.evaka.daycare.service.StaffAttendanceUpdate
*/
export interface StaffAttendanceUpdate {
  count: number | null
  countOther: number | null
  date: LocalDate
  groupId: UUID
}

/**
* Generated from fi.espoo.evaka.daycare.service.Stats
*/
export interface Stats {
  maximum: number
  minimum: number
}

/**
* Generated from fi.espoo.evaka.daycare.UnitFeatures
*/
export interface UnitFeatures {
  features: PilotFeature[]
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.daycare.UnitManager
*/
export interface UnitManager {
  email: string | null
  name: string | null
  phone: string | null
}

/**
* Generated from fi.espoo.evaka.daycare.service.UnitStaffAttendance
*/
export interface UnitStaffAttendance {
  count: number
  countOther: number
  date: LocalDate
  groups: GroupStaffAttendance[]
  updated: Date | null
}

/**
* Generated from fi.espoo.evaka.daycare.UnitStub
*/
export interface UnitStub {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.daycare.VisitingAddress
*/
export interface VisitingAddress {
  postOffice: string
  postalCode: string
  streetAddress: string
}
