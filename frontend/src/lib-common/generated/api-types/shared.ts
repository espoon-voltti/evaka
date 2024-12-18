// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
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

export type DaycareAssistanceId = string

export type DaycareCaretakerId = string

export type DaycareId = Id<'Daycare'>

export type DecisionId = string

export type DocumentTemplateId = string

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

export type FeeAlterationId = string

export type FeeDecisionId = string

export type FeeThresholdsId = string

export type FosterParentId = string

export type GroupId = Id<'Group'>

export type GroupNoteId = Id<'GroupNote'>

export type GroupPlacementId = string

/**
* Generated from fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
*/
export interface HelsinkiDateTimeRange {
  end: HelsinkiDateTime
  start: HelsinkiDateTime
}

export type HolidayPeriodId = string

export type HolidayQuestionnaireId = string

export type IncomeId = string

export type IncomeStatementId = string

export type InvoiceCorrectionId = string

export type InvoiceId = string

export type InvoiceRowId = string

export type MessageAccountId = string

export type MessageContentId = string

export type MessageDraftId = string

export type MessageId = string

export type MessageThreadId = string

export type MobileDeviceId = string

/**
* Generated from fi.espoo.evaka.shared.domain.OfficialLanguage
*/
export const officialLanguages = [
  'FI',
  'SV'
] as const

export type OfficialLanguage = typeof officialLanguages[number]

export type OtherAssistanceMeasureId = string

export type PairingId = string

export type ParentshipId = string

export type PartnershipId = string

export type PaymentId = string

export type PedagogicalDocumentId = string

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
  'SERVICE_APPLICATIONS'
] as const

export type PilotFeature = typeof pilotFeatures[number]

export type PlacementId = string

export type PlacementPlanId = string

export type PreschoolAssistanceId = string

export type PreschoolTermId = Id<'PreschoolTerm'>

export type ServiceApplicationId = string

export type ServiceNeedId = string

export type ServiceNeedOptionId = Id<'ServiceNeedOption'>

export type ServiceNeedOptionVoucherValueId = string

export type StaffAttendanceExternalId = string

export type StaffAttendanceRealtimeId = string

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

export type VoucherValueDecisionId = string

export type ChildId = PersonId


export function deserializeJsonHelsinkiDateTimeRange(json: JsonOf<HelsinkiDateTimeRange>): HelsinkiDateTimeRange {
  return {
    ...json,
    end: HelsinkiDateTime.parseIso(json.end),
    start: HelsinkiDateTime.parseIso(json.start)
  }
}
