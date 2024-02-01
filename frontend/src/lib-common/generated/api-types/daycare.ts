// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import DateRange from '../../date-range'
import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { Coordinate } from './shared'
import { DaycareAclRow } from './shared'
import { DaycarePlacementWithDetails } from './placement'
import { MissingGroupPlacement } from './placement'
import { OccupancyResponse } from './occupancy'
import { PersonJSON } from './pis'
import { PilotFeature } from './shared'
import { TerminatedPlacement } from './placement'
import { TimeRange } from './shared'
import { UUID } from '../../types'
import { UnitBackupCare } from './backupcare'
import { UnitChildrenCapacityFactors } from './placement'

/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.AclUpdate
*/
export interface AclUpdate {
  groupIds: UUID[] | null
  hasStaffOccupancyEffect: boolean | null
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.AdditionalInformation
*/
export interface AdditionalInformation {
  additionalInfo: string
  allergies: string
  diet: string
  languageAtHome: string
  languageAtHomeDetails: string
  medication: string
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.ApplicationUnitType
*/
export type ApplicationUnitType =
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'
  | 'PREPARATORY'

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
export const careTypes = [
  'CLUB',
  'FAMILY',
  'CENTRE',
  'GROUP_FAMILY',
  'PRESCHOOL',
  'PREPARATORY_EDUCATION'
] as const

export type CareType = typeof careTypes[number]

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
* Generated from fi.espoo.evaka.daycare.service.Caretakers
*/
export interface Caretakers {
  maximum: number
  minimum: number
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
* Generated from fi.espoo.evaka.daycare.controllers.ChildController.ChildResponse
*/
export interface ChildResponse {
  assistanceNeedVoucherCoefficientsEnabled: boolean
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
  termBreaks: FiniteDateRange[]
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
  businessId: string
  capacity: number
  closingDate: LocalDate | null
  clubApplyPeriod: DateRange | null
  costCenter: string | null
  dailyPreparatoryTime: TimeRange | null
  dailyPreschoolTime: TimeRange | null
  daycareApplyPeriod: DateRange | null
  decisionCustomization: DaycareDecisionCustomization
  dwCostCenter: string | null
  email: string | null
  enabledPilotFeatures: PilotFeature[]
  financeDecisionHandler: FinanceDecisionHandler | null
  ghostUnit: boolean
  iban: string
  id: UUID
  invoicedByMunicipality: boolean
  language: Language
  location: Coordinate | null
  mailingAddress: MailingAddress
  name: string
  openingDate: LocalDate | null
  operationDays: number[]
  operationTimes: (TimeRange | null)[]
  ophOrganizerOid: string | null
  ophUnitOid: string | null
  phone: string | null
  preschoolApplyPeriod: DateRange | null
  providerId: string
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
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.DaycareAclResponse
*/
export interface DaycareAclResponse {
  aclRows: DaycareAclRow[]
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
  businessId: string
  capacity: number
  closingDate: LocalDate | null
  clubApplyPeriod: DateRange | null
  costCenter: string | null
  dailyPreparatoryTime: TimeRange | null
  dailyPreschoolTime: TimeRange | null
  daycareApplyPeriod: DateRange | null
  decisionCustomization: DaycareDecisionCustomization
  dwCostCenter: string | null
  email: string | null
  financeDecisionHandlerId: UUID | null
  ghostUnit: boolean
  iban: string
  invoicedByMunicipality: boolean
  language: Language
  location: Coordinate | null
  mailingAddress: MailingAddress
  name: string
  openingDate: LocalDate | null
  operationTimes: (TimeRange | null)[]
  ophOrganizerOid: string | null
  ophUnitOid: string | null
  phone: string | null
  preschoolApplyPeriod: DateRange | null
  providerId: string
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
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.DaycareGroupResponse
*/
export interface DaycareGroupResponse {
  endDate: LocalDate | null
  id: UUID
  name: string
  permittedActions: Action.Group[]
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.DaycareResponse
*/
export interface DaycareResponse {
  daycare: Daycare
  groups: DaycareGroupResponse[]
  permittedActions: Action.Unit[]
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
* Generated from fi.espoo.evaka.daycare.controllers.GroupOccupancies
*/
export interface GroupOccupancies {
  confirmed: Record<string, OccupancyResponse>
  realized: Record<string, OccupancyResponse>
}

/**
* Generated from fi.espoo.evaka.daycare.service.GroupStaffAttendance
*/
export interface GroupStaffAttendance {
  count: number
  countOther: number
  date: LocalDate
  groupId: UUID
  updated: HelsinkiDateTime
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
  termBreaks: FiniteDateRange[]
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
* Generated from fi.espoo.evaka.daycare.service.StaffAttendanceForDates
*/
export interface StaffAttendanceForDates {
  attendances: Record<string, GroupStaffAttendance>
  endDate: LocalDate | null
  groupId: UUID
  groupName: string
  startDate: LocalDate
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
* Generated from fi.espoo.evaka.daycare.UnitFeatures
*/
export interface UnitFeatures {
  features: PilotFeature[]
  id: UUID
  name: string
  providerType: ProviderType
  type: CareType[]
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitGroupDetails
*/
export interface UnitGroupDetails {
  backupCares: UnitBackupCare[]
  caretakers: Record<string, Caretakers>
  groupOccupancies: GroupOccupancies | null
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  permittedBackupCareActions: Record<string, Action.BackupCare[]>
  permittedGroupPlacementActions: Record<string, Action.GroupPlacement[]>
  permittedPlacementActions: Record<string, Action.Placement[]>
  placements: DaycarePlacementWithDetails[]
  recentlyTerminatedPlacements: TerminatedPlacement[]
  unitChildrenCapacityFactors: UnitChildrenCapacityFactors[]
}

/**
* Generated from fi.espoo.evaka.daycare.UnitManager
*/
export interface UnitManager {
  email: string
  name: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.UnitNotifications
*/
export interface UnitNotifications {
  applications: number
  groups: number
}

/**
* Generated from fi.espoo.evaka.daycare.service.UnitStaffAttendance
*/
export interface UnitStaffAttendance {
  count: number
  countOther: number
  date: LocalDate
  groups: GroupStaffAttendance[]
  updated: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.daycare.UnitStub
*/
export interface UnitStub {
  careTypes: CareType[]
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitTypeFilter
*/
export type UnitTypeFilter =
  | 'ALL'
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'

/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.UpdateFeaturesRequest
*/
export interface UpdateFeaturesRequest {
  enable: boolean
  features: PilotFeature[]
  unitIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.daycare.VisitingAddress
*/
export interface VisitingAddress {
  postOffice: string
  postalCode: string
  streetAddress: string
}
