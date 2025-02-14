// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Id } from '../../id-type'
import { JsonOf } from '../../json'

export type ApplicationId = Id<'Application'>

export type ApplicationNoteId = Id<'ApplicationNote'>

export type ArchivedProcessId = Id<'ArchivedProcess'>

export type AreaId = Id<'Area'>

export type AssistanceActionId = Id<'AssistanceAction'>

export type AssistanceFactorId = Id<'AssistanceFactor'>

export type AssistanceNeedDecisionGuardianId = Id<'AssistanceNeedDecisionGuardian'>

export type AssistanceNeedDecisionId = Id<'AssistanceNeedDecision'>

export type AssistanceNeedPreschoolDecisionGuardianId = Id<'AssistanceNeedPreschoolDecisionGuardian'>

export type AssistanceNeedPreschoolDecisionId = Id<'AssistanceNeedPreschoolDecision'>

export type AssistanceNeedVoucherCoefficientId = Id<'AssistanceNeedVoucherCoefficient'>

export type AttachmentId = Id<'Attachment'>

export type BackupCareId = Id<'BackupCare'>

export type BackupPickupId = Id<'BackupPickup'>

export type CalendarEventId = Id<'CalendarEvent'>

export type CalendarEventTimeId = Id<'CalendarEventTime'>

export type ChildDailyNoteId = Id<'ChildDailyNote'>

export type ChildDocumentId = Id<'ChildDocument'>

export type ChildImageId = Id<'ChildImage'>

export type ChildStickyNoteId = Id<'ChildStickyNote'>

/**
* Generated from fi.espoo.evaka.shared.auth.CitizenAuthLevel
*/
export type CitizenAuthLevel =
  | 'WEAK'
  | 'STRONG'

/**
* Generated from fi.espoo.evaka.shared.security.CitizenFeatures
*/
export interface CitizenFeatures {
  childDocumentation: boolean
  composeNewMessage: boolean
  messages: boolean
  reservations: boolean
}

export type ClubTermId = Id<'ClubTerm'>

/**
* Generated from fi.espoo.evaka.shared.domain.Coordinate
*/
export interface Coordinate {
  lat: number
  lon: number
}

export type DailyServiceTimeId = Id<'DailyServiceTime'>

export type DailyServiceTimeNotificationId = Id<'DailyServiceTimeNotification'>

/**
* Generated from fi.espoo.evaka.shared.auth.DaycareAclRow
*/
export interface DaycareAclRow {
  employee: DaycareAclRowEmployee
  endDate: LocalDate | null
  groupIds: GroupId[]
  role: UserRole
}

/**
* Generated from fi.espoo.evaka.shared.auth.DaycareAclRowEmployee
*/
export interface DaycareAclRowEmployee {
  active: boolean
  email: string | null
  firstName: string
  hasStaffOccupancyEffect: boolean | null
  id: EmployeeId
  lastName: string
  temporary: boolean
}

export type DaycareAssistanceId = Id<'DaycareAssistance'>

export type DaycareCaretakerId = Id<'DaycareCaretaker'>

export type DaycareId = Id<'Daycare'>

export type DecisionId = Id<'Decision'>

export type DocumentTemplateId = Id<'DocumentTemplate'>

/**
* Generated from fi.espoo.evaka.shared.security.EmployeeFeatures
*/
export interface EmployeeFeatures {
  applications: boolean
  assistanceNeedDecisionsReport: boolean
  createDraftInvoices: boolean
  createPlacements: boolean
  createUnits: boolean
  documentTemplates: boolean
  employees: boolean
  finance: boolean
  financeBasics: boolean
  holidayAndTermPeriods: boolean
  messages: boolean
  openRangesHolidayQuestionnaire: boolean
  outOfOffice: boolean
  personSearch: boolean
  personalMobileDevice: boolean
  pinCode: boolean
  placementTool: boolean
  replacementInvoices: boolean
  reports: boolean
  settings: boolean
  submitPatuReport: boolean
  systemNotifications: boolean
  unitFeatures: boolean
  units: boolean
}

export type EmployeeId = Id<'Employee'>

export type EvakaUserId = Id<'EvakaUser'>

export type FeeAlterationId = Id<'FeeAlteration'>

export type FeeDecisionId = Id<'FeeDecision'>

export type FeeThresholdsId = Id<'FeeThresholds'>

export type FinanceNoteId = Id<'FinanceNote'>

export type FosterParentId = Id<'FosterParent'>

export type GroupId = Id<'Group'>

export type GroupNoteId = Id<'GroupNote'>

export type GroupPlacementId = Id<'GroupPlacement'>

/**
* Generated from fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
*/
export interface HelsinkiDateTimeRange {
  end: HelsinkiDateTime
  start: HelsinkiDateTime
}

export type HolidayPeriodId = Id<'HolidayPeriod'>

export type HolidayQuestionnaireId = Id<'HolidayQuestionnaire'>

export type IncomeId = Id<'Income'>

export type IncomeStatementId = Id<'IncomeStatement'>

export type InvoiceCorrectionId = Id<'InvoiceCorrection'>

export type InvoiceId = Id<'Invoice'>

export type InvoiceRowId = Id<'InvoiceRow'>

export type MessageAccountId = Id<'MessageAccount'>

export type MessageContentId = Id<'MessageContent'>

export type MessageDraftId = Id<'MessageDraft'>

export type MessageId = Id<'Message'>

export type MessageThreadId = Id<'MessageThread'>

export type MobileDeviceId = Id<'MobileDevice'>

/**
* Generated from fi.espoo.evaka.shared.domain.OfficialLanguage
*/
export const officialLanguages = [
  'FI',
  'SV'
] as const

export type OfficialLanguage = typeof officialLanguages[number]

export type OtherAssistanceMeasureId = Id<'OtherAssistanceMeasure'>

export type OutOfOfficeId = Id<'OutOfOffice'>

export type PairingId = Id<'Pairing'>

export type ParentshipId = Id<'Parentship'>

export type PartnershipId = Id<'Partnership'>

/**
* Generated from fi.espoo.evaka.shared.auth.PasswordConstraints
*/
export interface PasswordConstraints {
  maxLength: number
  minDigits: number
  minLength: number
  minLowers: number
  minSymbols: number
  minUppers: number
}

export type PaymentId = Id<'Payment'>

export type PedagogicalDocumentId = Id<'PedagogicalDocument'>

export type PersonEmailVerificationId = Id<'PersonEmailVerification'>

export type PersonId = Id<'Person'>

/**
* Generated from fi.espoo.evaka.shared.security.PilotFeature
*/
export const pilotFeatures = [
  'MESSAGING',
  'MOBILE',
  'RESERVATIONS',
  'VASU_AND_PEDADOC',
  'MOBILE_MESSAGING',
  'PLACEMENT_TERMINATION',
  'REALTIME_STAFF_ATTENDANCE',
  'PUSH_NOTIFICATIONS',
  'SERVICE_APPLICATIONS',
  'STAFF_ATTENDANCE_INTEGRATION'
] as const

export type PilotFeature = typeof pilotFeatures[number]

export type PlacementId = Id<'Placement'>

export type PlacementPlanId = Id<'PlacementPlan'>

export type PreschoolAssistanceId = Id<'PreschoolAssistance'>

export type PreschoolTermId = Id<'PreschoolTerm'>

/**
* Generated from fi.espoo.evaka.shared.auth.ScheduledDaycareAclRow
*/
export interface ScheduledDaycareAclRow {
  email: string | null
  endDate: LocalDate | null
  firstName: string
  id: EmployeeId
  lastName: string
  role: UserRole
  startDate: LocalDate
}

export type ServiceApplicationId = Id<'ServiceApplication'>

export type ServiceNeedId = Id<'ServiceNeed'>

export type ServiceNeedOptionId = Id<'ServiceNeedOption'>

export type ServiceNeedOptionVoucherValueId = Id<'ServiceNeedOptionVoucherValue'>

export type StaffAttendanceExternalId = Id<'StaffAttendanceExternal'>

export type StaffAttendanceRealtimeId = Id<'StaffAttendanceRealtime'>

export type TitaniaErrorsId = Id<'TitaniaErrors'>

/**
* Generated from fi.espoo.evaka.shared.domain.Translatable
*/
export interface Translatable {
  en: string
  fi: string
  sv: string
}

/**
* Generated from fi.espoo.evaka.shared.auth.UserRole
*/
export type UserRole =
  | 'ADMIN'
  | 'REPORT_VIEWER'
  | 'DIRECTOR'
  | 'FINANCE_ADMIN'
  | 'FINANCE_STAFF'
  | 'SERVICE_WORKER'
  | 'MESSAGING'
  | 'UNIT_SUPERVISOR'
  | 'STAFF'
  | 'SPECIAL_EDUCATION_TEACHER'
  | 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'

export type VoucherValueDecisionId = Id<'VoucherValueDecision'>

export type ChildId = PersonId


export function deserializeJsonDaycareAclRow(json: JsonOf<DaycareAclRow>): DaycareAclRow {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null
  }
}


export function deserializeJsonHelsinkiDateTimeRange(json: JsonOf<HelsinkiDateTimeRange>): HelsinkiDateTimeRange {
  return {
    ...json,
    end: HelsinkiDateTime.parseIso(json.end),
    start: HelsinkiDateTime.parseIso(json.start)
  }
}


export function deserializeJsonScheduledDaycareAclRow(json: JsonOf<ScheduledDaycareAclRow>): ScheduledDaycareAclRow {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}
