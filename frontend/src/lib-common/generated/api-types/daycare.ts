// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from '../../date-range'
import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import TimeRange from '../../time-range'
import { Action } from '../action'
import { Coordinate } from './shared'
import { DaycarePlacementWithDetails } from './placement'
import { JsonOf } from '../../json'
import { MissingGroupPlacement } from './placement'
import { OccupancyResponse } from './occupancy'
import { PersonJSON } from './pis'
import { PilotFeature } from './shared'
import { SpecialDiet } from './specialdiet'
import { TerminatedPlacement } from './placement'
import { UUID } from '../../types'
import { UnitBackupCare } from './backupcare'
import { UnitChildrenCapacityFactors } from './placement'
import { UserRole } from './shared'
import { deserializeJsonDaycarePlacementWithDetails } from './placement'
import { deserializeJsonMissingGroupPlacement } from './placement'
import { deserializeJsonOccupancyResponse } from './occupancy'
import { deserializeJsonPersonJSON } from './pis'
import { deserializeJsonTerminatedPlacement } from './placement'
import { deserializeJsonUnitBackupCare } from './backupcare'

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
  specialDiet: SpecialDiet | null
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
  id: UUID
  term: FiniteDateRange
  termBreaks: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.ClubTermRequest
*/
export interface ClubTermRequest {
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
  mealtimeBreakfast: TimeRange | null
  mealtimeEveningSnack: TimeRange | null
  mealtimeLunch: TimeRange | null
  mealtimeSnack: TimeRange | null
  mealtimeSupper: TimeRange | null
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
  mealtimeBreakfast: TimeRange | null
  mealtimeEveningSnack: TimeRange | null
  mealtimeLunch: TimeRange | null
  mealtimeSnack: TimeRange | null
  mealtimeSupper: TimeRange | null
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
  jamixCustomerId: number | null
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
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.FullAclInfo
*/
export interface FullAclInfo {
  role: UserRole
  update: AclUpdate
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.GroupOccupancies
*/
export interface GroupOccupancies {
  confirmed: Record<UUID, OccupancyResponse>
  realized: Record<UUID, OccupancyResponse>
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
  jamixCustomerId: number | null
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
  id: UUID
  swedishPreschool: FiniteDateRange
  termBreaks: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.PreschoolTermRequest
*/
export interface PreschoolTermRequest {
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
  caretakers: Record<UUID, Caretakers>
  groupOccupancies: GroupOccupancies | null
  groups: DaycareGroup[]
  missingGroupPlacements: MissingGroupPlacement[]
  permittedBackupCareActions: Record<UUID, Action.BackupCare[]>
  permittedGroupPlacementActions: Record<UUID, Action.GroupPlacement[]>
  permittedPlacementActions: Record<UUID, Action.Placement[]>
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


export function deserializeJsonCaretakerAmount(json: JsonOf<CaretakerAmount>): CaretakerAmount {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonCaretakerRequest(json: JsonOf<CaretakerRequest>): CaretakerRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonCaretakersResponse(json: JsonOf<CaretakersResponse>): CaretakersResponse {
  return {
    ...json,
    caretakers: json.caretakers.map(e => deserializeJsonCaretakerAmount(e))
  }
}


export function deserializeJsonChildResponse(json: JsonOf<ChildResponse>): ChildResponse {
  return {
    ...json,
    person: deserializeJsonPersonJSON(json.person)
  }
}


export function deserializeJsonClubTerm(json: JsonOf<ClubTerm>): ClubTerm {
  return {
    ...json,
    applicationPeriod: FiniteDateRange.parseJson(json.applicationPeriod),
    term: FiniteDateRange.parseJson(json.term),
    termBreaks: json.termBreaks.map((x) => FiniteDateRange.parseJson(x))
  }
}


export function deserializeJsonClubTermRequest(json: JsonOf<ClubTermRequest>): ClubTermRequest {
  return {
    ...json,
    applicationPeriod: FiniteDateRange.parseJson(json.applicationPeriod),
    term: FiniteDateRange.parseJson(json.term),
    termBreaks: json.termBreaks.map((x) => FiniteDateRange.parseJson(x))
  }
}


export function deserializeJsonCreateGroupRequest(json: JsonOf<CreateGroupRequest>): CreateGroupRequest {
  return {
    ...json,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDaycare(json: JsonOf<Daycare>): Daycare {
  return {
    ...json,
    closingDate: (json.closingDate != null) ? LocalDate.parseIso(json.closingDate) : null,
    clubApplyPeriod: (json.clubApplyPeriod != null) ? DateRange.parseJson(json.clubApplyPeriod) : null,
    dailyPreparatoryTime: (json.dailyPreparatoryTime != null) ? TimeRange.parseJson(json.dailyPreparatoryTime) : null,
    dailyPreschoolTime: (json.dailyPreschoolTime != null) ? TimeRange.parseJson(json.dailyPreschoolTime) : null,
    daycareApplyPeriod: (json.daycareApplyPeriod != null) ? DateRange.parseJson(json.daycareApplyPeriod) : null,
    mealtimeBreakfast: (json.mealtimeBreakfast != null) ? TimeRange.parseJson(json.mealtimeBreakfast) : null,
    mealtimeEveningSnack: (json.mealtimeEveningSnack != null) ? TimeRange.parseJson(json.mealtimeEveningSnack) : null,
    mealtimeLunch: (json.mealtimeLunch != null) ? TimeRange.parseJson(json.mealtimeLunch) : null,
    mealtimeSnack: (json.mealtimeSnack != null) ? TimeRange.parseJson(json.mealtimeSnack) : null,
    mealtimeSupper: (json.mealtimeSupper != null) ? TimeRange.parseJson(json.mealtimeSupper) : null,
    openingDate: (json.openingDate != null) ? LocalDate.parseIso(json.openingDate) : null,
    operationTimes: json.operationTimes.map(e => (e != null) ? TimeRange.parseJson(e) : null),
    preschoolApplyPeriod: (json.preschoolApplyPeriod != null) ? DateRange.parseJson(json.preschoolApplyPeriod) : null
  }
}


export function deserializeJsonDaycareFields(json: JsonOf<DaycareFields>): DaycareFields {
  return {
    ...json,
    closingDate: (json.closingDate != null) ? LocalDate.parseIso(json.closingDate) : null,
    clubApplyPeriod: (json.clubApplyPeriod != null) ? DateRange.parseJson(json.clubApplyPeriod) : null,
    dailyPreparatoryTime: (json.dailyPreparatoryTime != null) ? TimeRange.parseJson(json.dailyPreparatoryTime) : null,
    dailyPreschoolTime: (json.dailyPreschoolTime != null) ? TimeRange.parseJson(json.dailyPreschoolTime) : null,
    daycareApplyPeriod: (json.daycareApplyPeriod != null) ? DateRange.parseJson(json.daycareApplyPeriod) : null,
    mealtimeBreakfast: (json.mealtimeBreakfast != null) ? TimeRange.parseJson(json.mealtimeBreakfast) : null,
    mealtimeEveningSnack: (json.mealtimeEveningSnack != null) ? TimeRange.parseJson(json.mealtimeEveningSnack) : null,
    mealtimeLunch: (json.mealtimeLunch != null) ? TimeRange.parseJson(json.mealtimeLunch) : null,
    mealtimeSnack: (json.mealtimeSnack != null) ? TimeRange.parseJson(json.mealtimeSnack) : null,
    mealtimeSupper: (json.mealtimeSupper != null) ? TimeRange.parseJson(json.mealtimeSupper) : null,
    openingDate: (json.openingDate != null) ? LocalDate.parseIso(json.openingDate) : null,
    operationTimes: json.operationTimes.map(e => (e != null) ? TimeRange.parseJson(e) : null),
    preschoolApplyPeriod: (json.preschoolApplyPeriod != null) ? DateRange.parseJson(json.preschoolApplyPeriod) : null
  }
}


export function deserializeJsonDaycareGroup(json: JsonOf<DaycareGroup>): DaycareGroup {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDaycareGroupResponse(json: JsonOf<DaycareGroupResponse>): DaycareGroupResponse {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null
  }
}


export function deserializeJsonDaycareResponse(json: JsonOf<DaycareResponse>): DaycareResponse {
  return {
    ...json,
    daycare: deserializeJsonDaycare(json.daycare),
    groups: json.groups.map(e => deserializeJsonDaycareGroupResponse(e))
  }
}


export function deserializeJsonGroupOccupancies(json: JsonOf<GroupOccupancies>): GroupOccupancies {
  return {
    ...json,
    confirmed: Object.fromEntries(Object.entries(json.confirmed).map(
      ([k, v]) => [k, deserializeJsonOccupancyResponse(v)]
    )),
    realized: Object.fromEntries(Object.entries(json.realized).map(
      ([k, v]) => [k, deserializeJsonOccupancyResponse(v)]
    ))
  }
}


export function deserializeJsonGroupStaffAttendance(json: JsonOf<GroupStaffAttendance>): GroupStaffAttendance {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonGroupUpdateRequest(json: JsonOf<GroupUpdateRequest>): GroupUpdateRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonPreschoolTerm(json: JsonOf<PreschoolTerm>): PreschoolTerm {
  return {
    ...json,
    applicationPeriod: FiniteDateRange.parseJson(json.applicationPeriod),
    extendedTerm: FiniteDateRange.parseJson(json.extendedTerm),
    finnishPreschool: FiniteDateRange.parseJson(json.finnishPreschool),
    swedishPreschool: FiniteDateRange.parseJson(json.swedishPreschool),
    termBreaks: json.termBreaks.map((x) => FiniteDateRange.parseJson(x))
  }
}


export function deserializeJsonPreschoolTermRequest(json: JsonOf<PreschoolTermRequest>): PreschoolTermRequest {
  return {
    ...json,
    applicationPeriod: FiniteDateRange.parseJson(json.applicationPeriod),
    extendedTerm: FiniteDateRange.parseJson(json.extendedTerm),
    finnishPreschool: FiniteDateRange.parseJson(json.finnishPreschool),
    swedishPreschool: FiniteDateRange.parseJson(json.swedishPreschool),
    termBreaks: json.termBreaks.map((x) => FiniteDateRange.parseJson(x))
  }
}


export function deserializeJsonPublicUnit(json: JsonOf<PublicUnit>): PublicUnit {
  return {
    ...json,
    clubApplyPeriod: (json.clubApplyPeriod != null) ? DateRange.parseJson(json.clubApplyPeriod) : null,
    daycareApplyPeriod: (json.daycareApplyPeriod != null) ? DateRange.parseJson(json.daycareApplyPeriod) : null,
    preschoolApplyPeriod: (json.preschoolApplyPeriod != null) ? DateRange.parseJson(json.preschoolApplyPeriod) : null
  }
}


export function deserializeJsonStaffAttendanceForDates(json: JsonOf<StaffAttendanceForDates>): StaffAttendanceForDates {
  return {
    ...json,
    attendances: Object.fromEntries(Object.entries(json.attendances).map(
      ([k, v]) => [k, deserializeJsonGroupStaffAttendance(v)]
    )),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonStaffAttendanceUpdate(json: JsonOf<StaffAttendanceUpdate>): StaffAttendanceUpdate {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonUnitGroupDetails(json: JsonOf<UnitGroupDetails>): UnitGroupDetails {
  return {
    ...json,
    backupCares: json.backupCares.map(e => deserializeJsonUnitBackupCare(e)),
    groupOccupancies: (json.groupOccupancies != null) ? deserializeJsonGroupOccupancies(json.groupOccupancies) : null,
    groups: json.groups.map(e => deserializeJsonDaycareGroup(e)),
    missingGroupPlacements: json.missingGroupPlacements.map(e => deserializeJsonMissingGroupPlacement(e)),
    placements: json.placements.map(e => deserializeJsonDaycarePlacementWithDetails(e)),
    recentlyTerminatedPlacements: json.recentlyTerminatedPlacements.map(e => deserializeJsonTerminatedPlacement(e))
  }
}


export function deserializeJsonUnitStaffAttendance(json: JsonOf<UnitStaffAttendance>): UnitStaffAttendance {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    groups: json.groups.map(e => deserializeJsonGroupStaffAttendance(e)),
    updated: (json.updated != null) ? HelsinkiDateTime.parseIso(json.updated) : null
  }
}
