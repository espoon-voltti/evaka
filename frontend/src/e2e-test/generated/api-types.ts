// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AbsenceCategory } from 'lib-common/generated/api-types/absence'
import type { AbsenceType } from 'lib-common/generated/api-types/absence'
import type { ApplicationForm } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { ApplicationOrigin } from 'lib-common/generated/api-types/application'
import type { ApplicationStatus } from 'lib-common/generated/api-types/application'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import type { AreaId } from 'lib-common/generated/api-types/shared'
import type { AssistanceActionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceActionOptionCategory } from 'lib-common/generated/api-types/assistanceaction'
import type { AssistanceFactorId } from 'lib-common/generated/api-types/shared'
import type { AssistanceLevel } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionEmployee } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionGuardian } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedVoucherCoefficientId } from 'lib-common/generated/api-types/shared'
import type { BackupCareId } from 'lib-common/generated/api-types/shared'
import type { BackupPickupId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventTimeId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventType } from 'lib-common/generated/api-types/calendarevent'
import type { CareType } from 'lib-common/generated/api-types/daycare'
import type { CaseProcessId } from 'lib-common/generated/api-types/shared'
import type { ChildDocumentDecisionId } from 'lib-common/generated/api-types/shared'
import type { ChildDocumentDecisionStatus } from 'lib-common/generated/api-types/document'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import type { ChildDocumentType } from 'lib-common/generated/api-types/document'
import type { ChildWithDateOfBirth } from 'lib-common/generated/api-types/invoicing'
import type { ClubTermId } from 'lib-common/generated/api-types/shared'
import type { Coordinate } from 'lib-common/generated/api-types/shared'
import type { DailyServiceTimeId } from 'lib-common/generated/api-types/shared'
import type { DailyServiceTimeNotificationId } from 'lib-common/generated/api-types/shared'
import type { DailyServiceTimesType } from 'lib-common/generated/api-types/dailyservicetimes'
import DateRange from 'lib-common/date-range'
import type { DaycareAssistanceId } from 'lib-common/generated/api-types/shared'
import type { DaycareAssistanceLevel } from 'lib-common/generated/api-types/assistance'
import type { DaycareDecisionCustomization } from 'lib-common/generated/api-types/daycare'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { DecisionId } from 'lib-common/generated/api-types/shared'
import type { DecisionIncome } from 'lib-common/generated/api-types/invoicing'
import type { DecisionStatus } from 'lib-common/generated/api-types/decision'
import type { DecisionType } from 'lib-common/generated/api-types/decision'
import type { DocumentConfidentiality } from 'lib-common/generated/api-types/caseprocess'
import type { DocumentContent } from 'lib-common/generated/api-types/document'
import type { DocumentStatus } from 'lib-common/generated/api-types/document'
import type { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import type { DocumentTemplateId } from 'lib-common/generated/api-types/shared'
import type { EmailMessageType } from 'lib-common/generated/api-types/pis'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import type { EvakaUser } from 'lib-common/generated/api-types/user'
import type { EvakaUserId } from 'lib-common/generated/api-types/shared'
import type { FeeAlterationWithEffect } from 'lib-common/generated/api-types/invoicing'
import type { FeeDecisionThresholds } from 'lib-common/generated/api-types/invoicing'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { FosterParentId } from 'lib-common/generated/api-types/shared'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { GroupPlacementId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { HolidayQuestionnaireId } from 'lib-common/generated/api-types/shared'
import type { Id } from 'lib-common/id-type'
import type { IncomeEffect } from 'lib-common/generated/api-types/invoicing'
import type { IncomeId } from 'lib-common/generated/api-types/shared'
import type { IncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import type { IncomeStatementStatus } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeValue } from 'lib-common/generated/api-types/invoicing'
import type { InvoiceCorrectionId } from 'lib-common/generated/api-types/shared'
import type { InvoiceId } from 'lib-common/generated/api-types/shared'
import type { InvoiceRowId } from 'lib-common/generated/api-types/shared'
import type { InvoiceStatus } from 'lib-common/generated/api-types/invoicing'
import type { JsonOf } from 'lib-common/json'
import type { Language } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import type { MailingAddress } from 'lib-common/generated/api-types/daycare'
import type { MobileDeviceId } from 'lib-common/generated/api-types/shared'
import type { Nationality } from 'lib-common/generated/api-types/vtjclient'
import type { NativeLanguage } from 'lib-common/generated/api-types/vtjclient'
import type { NekkuProductMealType } from 'lib-common/generated/api-types/nekku'
import type { NekkuSpecialDietType } from 'lib-common/generated/api-types/nekku'
import type { OfficialLanguage } from 'lib-common/generated/api-types/shared'
import type { OtherAssistanceMeasureId } from 'lib-common/generated/api-types/shared'
import type { OtherAssistanceMeasureType } from 'lib-common/generated/api-types/assistance'
import type { ParentshipId } from 'lib-common/generated/api-types/shared'
import type { PartnershipId } from 'lib-common/generated/api-types/shared'
import type { PaymentId } from 'lib-common/generated/api-types/shared'
import type { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import type { PedagogicalDocumentId } from 'lib-common/generated/api-types/shared'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PilotFeature } from 'lib-common/generated/api-types/shared'
import type { PlacementId } from 'lib-common/generated/api-types/shared'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { PreschoolAssistanceId } from 'lib-common/generated/api-types/shared'
import type { PreschoolAssistanceLevel } from 'lib-common/generated/api-types/assistance'
import type { PreschoolTermId } from 'lib-common/generated/api-types/shared'
import type { ProviderType } from 'lib-common/generated/api-types/daycare'
import type { PushNotificationCategory } from 'lib-common/generated/api-types/webpush'
import type { ServiceNeedId } from 'lib-common/generated/api-types/shared'
import type { ServiceNeedOptionId } from 'lib-common/generated/api-types/shared'
import type { ServiceOptions } from 'lib-common/generated/api-types/assistanceneed'
import type { ShiftCareType } from 'lib-common/generated/api-types/serviceneed'
import type { StaffAttendanceRealtimeId } from 'lib-common/generated/api-types/shared'
import type { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import type { StructuralMotivationOptions } from 'lib-common/generated/api-types/assistanceneed'
import TimeRange from 'lib-common/time-range'
import type { UUID } from 'lib-common/types'
import type { UiLanguage } from 'lib-common/generated/api-types/shared'
import type { UnitManager } from 'lib-common/generated/api-types/daycare'
import type { UserRole } from 'lib-common/generated/api-types/shared'
import type { VisitingAddress } from 'lib-common/generated/api-types/daycare'
import type { VoucherValueDecisionDifference } from 'lib-common/generated/api-types/invoicing'
import type { VoucherValueDecisionId } from 'lib-common/generated/api-types/shared'
import type { VoucherValueDecisionServiceNeed } from 'lib-common/generated/api-types/invoicing'
import type { VoucherValueDecisionStatus } from 'lib-common/generated/api-types/invoicing'
import type { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonApplicationForm } from 'lib-common/generated/api-types/application'
import { deserializeJsonAssistanceNeedPreschoolDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonChildWithDateOfBirth } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonDocumentContent } from 'lib-common/generated/api-types/document'
import { deserializeJsonIncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'

export type AbsenceId = Id<'Absence'>

export type AssistanceActionOptionId = Id<'AssistanceActionOption'>

/**
* Generated from fi.espoo.evaka.shared.dev.MockDigitransit.Autocomplete
*/
export interface Autocomplete {
  features: Feature[]
}

export type CalendarEventAttendeeId = Id<'CalendarEventAttendee'>

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.Caretaker
*/
export interface Caretaker {
  amount: number
  endDate: LocalDate | null
  groupId: GroupId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.nekku.CustomerType
*/
export interface CustomerType {
  type: string
  weekdays: NekkuCustomerWeekday[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DaycareAclInsert
*/
export interface DaycareAclInsert {
  externalId: string
  role: UserRole | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DecisionRequest
*/
export interface DecisionRequest {
  applicationId: ApplicationId
  employeeId: EmployeeId
  endDate: LocalDate
  id: DecisionId
  startDate: LocalDate
  status: DecisionStatus
  type: DecisionType
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAbsence
*/
export interface DevAbsence {
  absenceCategory: AbsenceCategory
  absenceType: AbsenceType
  childId: PersonId
  date: LocalDate
  id: AbsenceId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  questionnaireId: HolidayQuestionnaireId | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApplicationWithForm
*/
export interface DevApplicationWithForm {
  allowOtherGuardianAccess: boolean
  checkedByAdmin: boolean
  childId: PersonId
  confidential: boolean | null
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  dueDate: LocalDate | null
  form: ApplicationForm
  guardianId: PersonId
  hideFromGuardian: boolean
  id: ApplicationId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  origin: ApplicationOrigin
  otherGuardians: PersonId[]
  sentDate: LocalDate | null
  status: ApplicationStatus
  transferApplication: boolean
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceAction
*/
export interface DevAssistanceAction {
  actions: string[]
  childId: PersonId
  createdAt: HelsinkiDateTime
  endDate: LocalDate
  id: AssistanceActionId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  otherAction: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceActionOption
*/
export interface DevAssistanceActionOption {
  category: AssistanceActionOptionCategory
  descriptionFi: string | null
  displayOrder: number | null
  id: AssistanceActionOptionId
  nameFi: string
  validFrom: LocalDate | null
  validTo: LocalDate | null
  value: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceFactor
*/
export interface DevAssistanceFactor {
  capacityFactor: number
  childId: PersonId
  id: AssistanceFactorId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
*/
export interface DevAssistanceNeedDecision {
  annulmentReason: string
  assistanceLevels: AssistanceLevel[]
  careMotivation: string | null
  childId: PersonId
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionEmployee | null
  decisionNumber: number | null
  endDateNotKnown: boolean
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  id: AssistanceNeedDecisionId
  language: OfficialLanguage
  motivationForDecision: string | null
  otherRepresentativeDetails: string | null
  otherRepresentativeHeard: boolean
  pedagogicalMotivation: string | null
  preparedBy1: AssistanceNeedDecisionEmployee | null
  preparedBy2: AssistanceNeedDecisionEmployee | null
  selectedUnit: DaycareId | null
  sentForDecision: LocalDate | null
  serviceOptions: ServiceOptions
  servicesMotivation: string | null
  status: AssistanceNeedDecisionStatus
  structuralMotivationDescription: string | null
  structuralMotivationOptions: StructuralMotivationOptions
  unreadGuardianIds: PersonId[] | null
  validityPeriod: DateRange
  viewOfGuardians: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceNeedPreschoolDecision
*/
export interface DevAssistanceNeedPreschoolDecision {
  annulmentReason: string
  childId: PersonId
  decisionMade: LocalDate | null
  decisionNumber: number
  form: AssistanceNeedPreschoolDecisionForm
  id: AssistanceNeedPreschoolDecisionId
  sentForDecision: LocalDate | null
  status: AssistanceNeedDecisionStatus
  unreadGuardianIds: PersonId[] | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceNeedVoucherCoefficient
*/
export interface DevAssistanceNeedVoucherCoefficient {
  childId: PersonId
  coefficient: number
  id: AssistanceNeedVoucherCoefficientId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  validityPeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevBackupCare
*/
export interface DevBackupCare {
  childId: PersonId
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  groupId: GroupId | null
  id: BackupCareId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  period: FiniteDateRange
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevBackupPickup
*/
export interface DevBackupPickup {
  childId: PersonId
  id: BackupPickupId
  name: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCalendarEvent
*/
export interface DevCalendarEvent {
  description: string
  eventType: CalendarEventType
  id: CalendarEventId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  period: FiniteDateRange
  title: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCalendarEventAttendee
*/
export interface DevCalendarEventAttendee {
  calendarEventId: CalendarEventId
  childId: PersonId | null
  groupId: GroupId | null
  id: CalendarEventAttendeeId
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCalendarEventTime
*/
export interface DevCalendarEventTime {
  calendarEventId: CalendarEventId
  childId: PersonId | null
  date: LocalDate
  end: LocalTime
  id: CalendarEventTimeId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  start: LocalTime
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCareArea
*/
export interface DevCareArea {
  areaCode: number | null
  id: AreaId
  name: string
  shortName: string
  subCostCenter: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevChild
*/
export interface DevChild {
  additionalInfo: string
  allergies: string
  diet: string
  dietId: number | null
  id: PersonId
  languageAtHome: string
  languageAtHomeDetails: string
  mealTextureId: number | null
  medication: string
  nekkuDiet: NekkuProductMealType | null
  participatesInBreakfast: boolean
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevChildAttendance
*/
export interface DevChildAttendance {
  arrived: LocalTime
  childId: PersonId
  date: LocalDate
  departed: LocalTime | null
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevChildDocument
*/
export interface DevChildDocument {
  answeredAt: HelsinkiDateTime | null
  answeredBy: EvakaUserId | null
  childId: PersonId
  content: DocumentContent
  contentModifiedAt: HelsinkiDateTime
  contentModifiedBy: EmployeeId | null
  created: HelsinkiDateTime | null
  createdBy: EvakaUserId
  decision: DevChildDocumentDecision | null
  decisionMaker: EmployeeId | null
  documentKey: string | null
  id: ChildDocumentId
  modifiedAt: HelsinkiDateTime
  processId: CaseProcessId | null
  publishedAt: HelsinkiDateTime | null
  publishedContent: DocumentContent | null
  status: DocumentStatus
  templateId: DocumentTemplateId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevChildDocumentDecision
*/
export interface DevChildDocumentDecision {
  createdAt: HelsinkiDateTime
  createdBy: EmployeeId
  daycareId: DaycareId | null
  id: ChildDocumentDecisionId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EmployeeId
  status: ChildDocumentDecisionStatus
  validity: DateRange | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevClubTerm
*/
export interface DevClubTerm {
  applicationPeriod: FiniteDateRange
  id: ClubTermId
  term: FiniteDateRange
  termBreaks: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDailyServiceTimeNotification
*/
export interface DevDailyServiceTimeNotification {
  guardianId: PersonId
  id: DailyServiceTimeNotificationId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDailyServiceTimes
*/
export interface DevDailyServiceTimes {
  childId: PersonId
  fridayTimes: TimeRange | null
  id: DailyServiceTimeId
  mondayTimes: TimeRange | null
  regularTimes: TimeRange | null
  saturdayTimes: TimeRange | null
  sundayTimes: TimeRange | null
  thursdayTimes: TimeRange | null
  tuesdayTimes: TimeRange | null
  type: DailyServiceTimesType
  validityPeriod: DateRange
  wednesdayTimes: TimeRange | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycare
*/
export interface DevDaycare {
  additionalInfo: string | null
  areaId: AreaId
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
  financeDecisionHandler: EmployeeId | null
  ghostUnit: boolean
  iban: string
  id: DaycareId
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
  partnerCode: string
  phone: string | null
  preschoolApplyPeriod: DateRange | null
  providerId: string
  providerType: ProviderType
  shiftCareOpenOnHolidays: boolean
  shiftCareOperationTimes: (TimeRange | null)[] | null
  type: CareType[]
  unitManager: UnitManager
  uploadChildrenToVarda: boolean
  uploadToKoski: boolean
  uploadToVarda: boolean
  url: string | null
  visitingAddress: VisitingAddress
  withSchool: boolean
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareAssistance
*/
export interface DevDaycareAssistance {
  childId: PersonId
  id: DaycareAssistanceId
  level: DaycareAssistanceLevel
  modified: HelsinkiDateTime
  modifiedBy: EvakaUser
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareGroup
*/
export interface DevDaycareGroup {
  aromiCustomerId: string | null
  daycareId: DaycareId
  endDate: LocalDate | null
  id: GroupId
  jamixCustomerNumber: number | null
  name: string
  nekkuCustomerNumber: string | null
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareGroupAcl
*/
export interface DevDaycareGroupAcl {
  created: HelsinkiDateTime
  employeeId: EmployeeId
  groupId: GroupId
  updated: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
*/
export interface DevDaycareGroupPlacement {
  daycareGroupId: GroupId
  daycarePlacementId: PlacementId
  endDate: LocalDate
  id: GroupPlacementId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDocumentTemplate
*/
export interface DevDocumentTemplate {
  archiveDurationMonths: number | null
  archiveExternally: boolean
  confidentiality: DocumentConfidentiality | null
  content: DocumentTemplateContent
  endDecisionWhenUnitChanges: boolean | null
  id: DocumentTemplateId
  language: UiLanguage
  legalBasis: string
  name: string
  placementTypes: PlacementType[]
  processDefinitionNumber: string | null
  published: boolean
  type: ChildDocumentType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevEmployee
*/
export interface DevEmployee {
  active: boolean
  email: string | null
  employeeNumber: string | null
  externalId: string | null
  firstName: string
  id: EmployeeId
  lastLogin: HelsinkiDateTime
  lastName: string
  preferredFirstName: string | null
  roles: UserRole[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevEmployeePin
*/
export interface DevEmployeePin {
  employeeExternalId: string | null
  id: UUID
  locked: boolean
  pin: string
  userId: EmployeeId | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFamilyContact
*/
export interface DevFamilyContact {
  childId: PersonId
  contactPersonId: PersonId
  id: UUID
  priority: number
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevFinanceNote
*/
export interface DevFinanceNote {
  content: string
  personId: PersonId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFosterParent
*/
export interface DevFosterParent {
  childId: PersonId
  id: FosterParentId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  parentId: PersonId
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFridgeChild
*/
export interface DevFridgeChild {
  childId: PersonId
  conflict: boolean
  endDate: LocalDate
  headOfChild: PersonId
  id: ParentshipId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFridgePartner
*/
export interface DevFridgePartner {
  conflict: boolean
  createdAt: HelsinkiDateTime
  endDate: LocalDate | null
  indx: number
  otherIndx: number
  partnershipId: PartnershipId
  personId: PersonId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevGuardian
*/
export interface DevGuardian {
  childId: PersonId
  guardianId: PersonId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevIncome
*/
export interface DevIncome {
  data: Partial<Record<string, IncomeValue>>
  effect: IncomeEffect
  id: IncomeId
  isEntrepreneur: boolean
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  personId: PersonId
  validFrom: LocalDate
  validTo: LocalDate | null
  worksAtEcha: boolean
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevIncomeStatement
*/
export interface DevIncomeStatement {
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  data: IncomeStatementBody
  handledAt: HelsinkiDateTime | null
  handlerId: EmployeeId | null
  id: IncomeStatementId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  personId: PersonId
  sentAt: HelsinkiDateTime | null
  status: IncomeStatementStatus
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevInvoice
*/
export interface DevInvoice {
  areaId: AreaId
  codebtor: PersonId | null
  createdAt: HelsinkiDateTime | null
  dueDate: LocalDate
  headOfFamilyId: PersonId
  id: InvoiceId
  invoiceDate: LocalDate
  number: number | null
  periodEnd: LocalDate
  periodStart: LocalDate
  replacedInvoiceId: InvoiceId | null
  revisionNumber: number
  rows: DevInvoiceRow[]
  sentAt: HelsinkiDateTime | null
  sentBy: EvakaUserId | null
  status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevInvoiceRow
*/
export interface DevInvoiceRow {
  amount: number
  childId: PersonId
  correctionId: InvoiceCorrectionId | null
  description: string
  id: InvoiceRowId
  idx: number | null
  periodEnd: LocalDate
  periodStart: LocalDate
  product: string
  unitId: DaycareId
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevMobileDevice
*/
export interface DevMobileDevice {
  id: MobileDeviceId
  longTermToken: UUID | null
  name: string
  pushNotificationCategories: PushNotificationCategory[]
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevOtherAssistanceMeasure
*/
export interface DevOtherAssistanceMeasure {
  childId: PersonId
  id: OtherAssistanceMeasureId
  modified: HelsinkiDateTime
  modifiedBy: EvakaUser
  type: OtherAssistanceMeasureType
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevParentship
*/
export interface DevParentship {
  childId: PersonId
  createdAt: HelsinkiDateTime
  endDate: LocalDate
  headOfChildId: PersonId
  id: ParentshipId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPayment
*/
export interface DevPayment {
  amount: number
  dueDate: LocalDate | null
  id: PaymentId
  number: number
  paymentDate: LocalDate | null
  period: FiniteDateRange
  sentAt: HelsinkiDateTime | null
  sentBy: EmployeeId | null
  status: PaymentStatus
  unitBusinessId: string | null
  unitIban: string | null
  unitId: DaycareId
  unitName: string
  unitPartnerCode: string | null
  unitProviderId: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPedagogicalDocument
*/
export interface DevPedagogicalDocument {
  childId: PersonId
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  description: string
  id: PedagogicalDocumentId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPerson
*/
export interface DevPerson {
  backupPhone: string
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  disabledEmailTypes: EmailMessageType[]
  duplicateOf: PersonId | null
  email: string | null
  firstName: string
  forceManualFeeDecisions: boolean
  id: PersonId
  invoiceRecipientName: string
  invoicingPostOffice: string
  invoicingPostalCode: string
  invoicingStreetAddress: string
  language: string | null
  lastName: string
  municipalityOfResidence: string
  nationalities: string[]
  ophPersonOid: string | null
  phone: string
  postOffice: string
  postalCode: string
  preferredName: string
  residenceCode: string
  restrictedDetailsEnabled: boolean
  restrictedDetailsEndDate: LocalDate | null
  ssn: string | null
  ssnAddingDisabled: boolean | null
  streetAddress: string
  updatedFromVtj: HelsinkiDateTime | null
  verifiedEmail: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevPersonEmail
*/
export interface DevPersonEmail {
  email: string | null
  personId: PersonId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPersonType
*/
export type DevPersonType =
  | 'CHILD'
  | 'ADULT'
  | 'RAW_ROW'

/**
* Generated from fi.espoo.evaka.shared.dev.DevPersonalMobileDevice
*/
export interface DevPersonalMobileDevice {
  employeeId: EmployeeId
  id: MobileDeviceId
  longTermToken: UUID | null
  name: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPlacement
*/
export interface DevPlacement {
  childId: PersonId
  endDate: LocalDate
  id: PlacementId
  placeGuarantee: boolean
  startDate: LocalDate
  terminatedBy: EvakaUserId | null
  terminationRequestedDate: LocalDate | null
  type: PlacementType
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPreschoolAssistance
*/
export interface DevPreschoolAssistance {
  childId: PersonId
  id: PreschoolAssistanceId
  level: PreschoolAssistanceLevel
  modified: HelsinkiDateTime
  modifiedBy: EvakaUser
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPreschoolTerm
*/
export interface DevPreschoolTerm {
  applicationPeriod: FiniteDateRange
  extendedTerm: FiniteDateRange
  finnishPreschool: FiniteDateRange
  id: PreschoolTermId
  swedishPreschool: FiniteDateRange
  termBreaks: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevServiceNeed
*/
export interface DevServiceNeed {
  confirmedAt: HelsinkiDateTime | null
  confirmedBy: EvakaUserId
  endDate: LocalDate
  id: ServiceNeedId
  optionId: ServiceNeedOptionId
  partWeek: boolean
  placementId: PlacementId
  shiftCare: ShiftCareType
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevStaffAttendance
*/
export interface DevStaffAttendance {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  departedAutomatically: boolean
  employeeId: EmployeeId
  groupId: GroupId | null
  id: StaffAttendanceRealtimeId
  modifiedAt: HelsinkiDateTime | null
  modifiedBy: EvakaUserId | null
  occupancyCoefficient: number
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevStaffAttendancePlan
*/
export interface DevStaffAttendancePlan {
  description: string | null
  employeeId: EmployeeId
  endTime: HelsinkiDateTime
  id: StaffAttendancePlanId
  startTime: HelsinkiDateTime
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevTerminatePlacementRequest
*/
export interface DevTerminatePlacementRequest {
  endDate: LocalDate
  placementId: PlacementId
  terminatedBy: EvakaUserId | null
  terminationRequestedDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevUpsertStaffOccupancyCoefficient
*/
export interface DevUpsertStaffOccupancyCoefficient {
  coefficient: number
  employeeId: EmployeeId
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.emailclient.Email
*/
export interface Email {
  content: EmailContent
  fromAddress: FromAddress
  toAddress: string
  traceId: string
}

/**
* Generated from fi.espoo.evaka.emailclient.EmailContent
*/
export interface EmailContent {
  html: string
  subject: string
  text: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.MockDigitransit.Feature
*/
export interface Feature {
  geometry: Geometry
  properties: FeatureProperties
}

/**
* Generated from fi.espoo.evaka.shared.dev.MockDigitransit.FeatureProperties
*/
export interface FeatureProperties {
  localadmin: string | null
  locality: string | null
  name: string
  postalcode: string | null
}

/**
* Generated from fi.espoo.evaka.emailclient.FromAddress
*/
export interface FromAddress {
  address: string
  arn: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.MockDigitransit.Geometry
*/
export interface Geometry {
  coordinates: [number, number]
}

/**
* Generated from fi.espoo.evaka.vtjclient.service.persondetails.MockVtjDataset
*/
export interface MockVtjDataset {
  guardianDependants: Partial<Record<string, string[]>>
  persons: MockVtjPerson[]
}

/**
* Generated from fi.espoo.evaka.vtjclient.service.persondetails.MockVtjPerson
*/
export interface MockVtjPerson {
  address: PersonAddress | null
  dateOfDeath: LocalDate | null
  firstNames: string
  lastName: string
  municipalityOfResidence: string | null
  nationalities: Nationality[]
  nativeLanguage: NativeLanguage | null
  residenceCode: string | null
  restrictedDetails: RestrictedDetails | null
  socialSecurityNumber: string
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuCustomer
*/
export interface NekkuCustomer {
  customerType: CustomerType[]
  group: string
  name: string
  number: string
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuCustomerWeekday
*/
export const nekku_customer_weekday = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
  'WEEKDAYHOLIDAY'
] as const

export type NekkuCustomerWeekday = typeof nekku_customer_weekday[number]

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDiet
*/
export interface NekkuSpecialDiet {
  fields: NekkuSpecialDietsField[]
  id: string
  name: string
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietOption
*/
export interface NekkuSpecialDietOption {
  key: string
  value: string
  weight: number
}

/**
* Generated from fi.espoo.evaka.nekku.NekkuSpecialDietsField
*/
export interface NekkuSpecialDietsField {
  id: string
  name: string
  options: NekkuSpecialDietOption[] | null
  type: NekkuSpecialDietType
}

/**
* Generated from fi.espoo.evaka.vtjclient.dto.PersonAddress
*/
export interface PersonAddress {
  postOffice: string | null
  postOfficeSe: string | null
  postalCode: string | null
  streetAddress: string | null
  streetAddressSe: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.PlacementPlan
*/
export interface PlacementPlan {
  periodEnd: LocalDate
  periodStart: LocalDate
  preschoolDaycarePeriodEnd: LocalDate | null
  preschoolDaycarePeriodStart: LocalDate | null
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationInsert
*/
export interface ReservationInsert {
  childId: PersonId
  date: LocalDate
  range: TimeRange | null
}

/**
* Generated from fi.espoo.evaka.vtjclient.dto.RestrictedDetails
*/
export interface RestrictedDetails {
  enabled: boolean
  endDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.sficlient.SfiMessage
*/
export interface SfiMessage {
  countryCode: string
  documentBucket: string
  documentDisplayName: string
  documentId: string
  documentKey: string
  emailContent: string | null
  emailHeader: string | null
  firstName: string
  lastName: string
  messageContent: string
  messageHeader: string
  messageId: SfiMessageId
  postOffice: string
  postalCode: string
  ssn: string
  streetAddress: string
}

export type SfiMessageId = Id<'SfiMessage'>

export type StaffAttendancePlanId = Id<'StaffAttendancePlan'>

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.UpdateIncomeStatementHandledBody
*/
export interface UpdateIncomeStatementHandledBody {
  employeeId: EmployeeId
  handled: boolean
  incomeStatementId: IncomeStatementId
  note: string
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecision
*/
export interface VoucherValueDecision {
  approvedAt: HelsinkiDateTime | null
  approvedById: EmployeeId | null
  assistanceNeedCoefficient: number
  baseCoPayment: number
  baseValue: number
  child: ChildWithDateOfBirth
  childIncome: DecisionIncome | null
  coPayment: number
  created: HelsinkiDateTime
  decisionHandler: UUID | null
  decisionNumber: number | null
  decisionType: VoucherValueDecisionType
  difference: VoucherValueDecisionDifference[]
  documentKey: string | null
  familySize: number
  feeAlterations: FeeAlterationWithEffect[]
  feeThresholds: FeeDecisionThresholds
  finalCoPayment: number
  headOfFamilyId: PersonId
  headOfFamilyIncome: DecisionIncome | null
  id: VoucherValueDecisionId
  partnerId: PersonId | null
  partnerIncome: DecisionIncome | null
  placement: VoucherValueDecisionPlacement | null
  sentAt: HelsinkiDateTime | null
  serviceNeed: VoucherValueDecisionServiceNeed | null
  siblingDiscount: number
  status: VoucherValueDecisionStatus
  validFrom: LocalDate
  validTo: LocalDate
  voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
*/
export interface VoucherValueDecisionPlacement {
  type: PlacementType
  unitId: DaycareId
}


export function deserializeJsonCaretaker(json: JsonOf<Caretaker>): Caretaker {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDecisionRequest(json: JsonOf<DecisionRequest>): DecisionRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevAbsence(json: JsonOf<DevAbsence>): DevAbsence {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonDevApplicationWithForm(json: JsonOf<DevApplicationWithForm>): DevApplicationWithForm {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    form: deserializeJsonApplicationForm(json.form),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    sentDate: (json.sentDate != null) ? LocalDate.parseIso(json.sentDate) : null
  }
}


export function deserializeJsonDevAssistanceAction(json: JsonOf<DevAssistanceAction>): DevAssistanceAction {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    endDate: LocalDate.parseIso(json.endDate),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevAssistanceActionOption(json: JsonOf<DevAssistanceActionOption>): DevAssistanceActionOption {
  return {
    ...json,
    validFrom: (json.validFrom != null) ? LocalDate.parseIso(json.validFrom) : null,
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonDevAssistanceFactor(json: JsonOf<DevAssistanceFactor>): DevAssistanceFactor {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validDuring: FiniteDateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonDevAssistanceNeedDecision(json: JsonOf<DevAssistanceNeedDecision>): DevAssistanceNeedDecision {
  return {
    ...json,
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    guardiansHeardOn: (json.guardiansHeardOn != null) ? LocalDate.parseIso(json.guardiansHeardOn) : null,
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null,
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonDevAssistanceNeedPreschoolDecision(json: JsonOf<DevAssistanceNeedPreschoolDecision>): DevAssistanceNeedPreschoolDecision {
  return {
    ...json,
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    form: deserializeJsonAssistanceNeedPreschoolDecisionForm(json.form),
    sentForDecision: (json.sentForDecision != null) ? LocalDate.parseIso(json.sentForDecision) : null
  }
}


export function deserializeJsonDevAssistanceNeedVoucherCoefficient(json: JsonOf<DevAssistanceNeedVoucherCoefficient>): DevAssistanceNeedVoucherCoefficient {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validityPeriod: FiniteDateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonDevBackupCare(json: JsonOf<DevBackupCare>): DevBackupCare {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonDevCalendarEvent(json: JsonOf<DevCalendarEvent>): DevCalendarEvent {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonDevCalendarEventTime(json: JsonOf<DevCalendarEventTime>): DevCalendarEventTime {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    end: LocalTime.parseIso(json.end),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    start: LocalTime.parseIso(json.start)
  }
}


export function deserializeJsonDevChildAttendance(json: JsonOf<DevChildAttendance>): DevChildAttendance {
  return {
    ...json,
    arrived: LocalTime.parseIso(json.arrived),
    date: LocalDate.parseIso(json.date),
    departed: (json.departed != null) ? LocalTime.parseIso(json.departed) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonDevChildDocument(json: JsonOf<DevChildDocument>): DevChildDocument {
  return {
    ...json,
    answeredAt: (json.answeredAt != null) ? HelsinkiDateTime.parseIso(json.answeredAt) : null,
    content: deserializeJsonDocumentContent(json.content),
    contentModifiedAt: HelsinkiDateTime.parseIso(json.contentModifiedAt),
    created: (json.created != null) ? HelsinkiDateTime.parseIso(json.created) : null,
    decision: (json.decision != null) ? deserializeJsonDevChildDocumentDecision(json.decision) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    publishedContent: (json.publishedContent != null) ? deserializeJsonDocumentContent(json.publishedContent) : null
  }
}


export function deserializeJsonDevChildDocumentDecision(json: JsonOf<DevChildDocumentDecision>): DevChildDocumentDecision {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validity: (json.validity != null) ? DateRange.parseJson(json.validity) : null
  }
}


export function deserializeJsonDevClubTerm(json: JsonOf<DevClubTerm>): DevClubTerm {
  return {
    ...json,
    applicationPeriod: FiniteDateRange.parseJson(json.applicationPeriod),
    term: FiniteDateRange.parseJson(json.term),
    termBreaks: json.termBreaks.map((x) => FiniteDateRange.parseJson(x))
  }
}


export function deserializeJsonDevDailyServiceTimes(json: JsonOf<DevDailyServiceTimes>): DevDailyServiceTimes {
  return {
    ...json,
    fridayTimes: (json.fridayTimes != null) ? TimeRange.parseJson(json.fridayTimes) : null,
    mondayTimes: (json.mondayTimes != null) ? TimeRange.parseJson(json.mondayTimes) : null,
    regularTimes: (json.regularTimes != null) ? TimeRange.parseJson(json.regularTimes) : null,
    saturdayTimes: (json.saturdayTimes != null) ? TimeRange.parseJson(json.saturdayTimes) : null,
    sundayTimes: (json.sundayTimes != null) ? TimeRange.parseJson(json.sundayTimes) : null,
    thursdayTimes: (json.thursdayTimes != null) ? TimeRange.parseJson(json.thursdayTimes) : null,
    tuesdayTimes: (json.tuesdayTimes != null) ? TimeRange.parseJson(json.tuesdayTimes) : null,
    validityPeriod: DateRange.parseJson(json.validityPeriod),
    wednesdayTimes: (json.wednesdayTimes != null) ? TimeRange.parseJson(json.wednesdayTimes) : null
  }
}


export function deserializeJsonDevDaycare(json: JsonOf<DevDaycare>): DevDaycare {
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
    preschoolApplyPeriod: (json.preschoolApplyPeriod != null) ? DateRange.parseJson(json.preschoolApplyPeriod) : null,
    shiftCareOperationTimes: (json.shiftCareOperationTimes != null) ? json.shiftCareOperationTimes.map(e => (e != null) ? TimeRange.parseJson(e) : null) : null
  }
}


export function deserializeJsonDevDaycareAssistance(json: JsonOf<DevDaycareAssistance>): DevDaycareAssistance {
  return {
    ...json,
    modified: HelsinkiDateTime.parseIso(json.modified),
    validDuring: FiniteDateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonDevDaycareGroup(json: JsonOf<DevDaycareGroup>): DevDaycareGroup {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevDaycareGroupAcl(json: JsonOf<DevDaycareGroupAcl>): DevDaycareGroupAcl {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonDevDaycareGroupPlacement(json: JsonOf<DevDaycareGroupPlacement>): DevDaycareGroupPlacement {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevDocumentTemplate(json: JsonOf<DevDocumentTemplate>): DevDocumentTemplate {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonDevEmployee(json: JsonOf<DevEmployee>): DevEmployee {
  return {
    ...json,
    lastLogin: HelsinkiDateTime.parseIso(json.lastLogin)
  }
}


export function deserializeJsonDevFosterParent(json: JsonOf<DevFosterParent>): DevFosterParent {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validDuring: DateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonDevFridgeChild(json: JsonOf<DevFridgeChild>): DevFridgeChild {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevFridgePartner(json: JsonOf<DevFridgePartner>): DevFridgePartner {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevIncome(json: JsonOf<DevIncome>): DevIncome {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonDevIncomeStatement(json: JsonOf<DevIncomeStatement>): DevIncomeStatement {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    data: deserializeJsonIncomeStatementBody(json.data),
    handledAt: (json.handledAt != null) ? HelsinkiDateTime.parseIso(json.handledAt) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonDevInvoice(json: JsonOf<DevInvoice>): DevInvoice {
  return {
    ...json,
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null,
    dueDate: LocalDate.parseIso(json.dueDate),
    invoiceDate: LocalDate.parseIso(json.invoiceDate),
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart),
    rows: json.rows.map(e => deserializeJsonDevInvoiceRow(e)),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonDevInvoiceRow(json: JsonOf<DevInvoiceRow>): DevInvoiceRow {
  return {
    ...json,
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart)
  }
}


export function deserializeJsonDevOtherAssistanceMeasure(json: JsonOf<DevOtherAssistanceMeasure>): DevOtherAssistanceMeasure {
  return {
    ...json,
    modified: HelsinkiDateTime.parseIso(json.modified),
    validDuring: FiniteDateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonDevParentship(json: JsonOf<DevParentship>): DevParentship {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevPayment(json: JsonOf<DevPayment>): DevPayment {
  return {
    ...json,
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    paymentDate: (json.paymentDate != null) ? LocalDate.parseIso(json.paymentDate) : null,
    period: FiniteDateRange.parseJson(json.period),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonDevPedagogicalDocument(json: JsonOf<DevPedagogicalDocument>): DevPedagogicalDocument {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonDevPerson(json: JsonOf<DevPerson>): DevPerson {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null,
    restrictedDetailsEndDate: (json.restrictedDetailsEndDate != null) ? LocalDate.parseIso(json.restrictedDetailsEndDate) : null,
    updatedFromVtj: (json.updatedFromVtj != null) ? HelsinkiDateTime.parseIso(json.updatedFromVtj) : null
  }
}


export function deserializeJsonDevPlacement(json: JsonOf<DevPlacement>): DevPlacement {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate),
    terminationRequestedDate: (json.terminationRequestedDate != null) ? LocalDate.parseIso(json.terminationRequestedDate) : null
  }
}


export function deserializeJsonDevPreschoolAssistance(json: JsonOf<DevPreschoolAssistance>): DevPreschoolAssistance {
  return {
    ...json,
    modified: HelsinkiDateTime.parseIso(json.modified),
    validDuring: FiniteDateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonDevPreschoolTerm(json: JsonOf<DevPreschoolTerm>): DevPreschoolTerm {
  return {
    ...json,
    applicationPeriod: FiniteDateRange.parseJson(json.applicationPeriod),
    extendedTerm: FiniteDateRange.parseJson(json.extendedTerm),
    finnishPreschool: FiniteDateRange.parseJson(json.finnishPreschool),
    swedishPreschool: FiniteDateRange.parseJson(json.swedishPreschool),
    termBreaks: json.termBreaks.map((x) => FiniteDateRange.parseJson(x))
  }
}


export function deserializeJsonDevServiceNeed(json: JsonOf<DevServiceNeed>): DevServiceNeed {
  return {
    ...json,
    confirmedAt: (json.confirmedAt != null) ? HelsinkiDateTime.parseIso(json.confirmedAt) : null,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevStaffAttendance(json: JsonOf<DevStaffAttendance>): DevStaffAttendance {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null,
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null
  }
}


export function deserializeJsonDevStaffAttendancePlan(json: JsonOf<DevStaffAttendancePlan>): DevStaffAttendancePlan {
  return {
    ...json,
    endTime: HelsinkiDateTime.parseIso(json.endTime),
    startTime: HelsinkiDateTime.parseIso(json.startTime)
  }
}


export function deserializeJsonDevTerminatePlacementRequest(json: JsonOf<DevTerminatePlacementRequest>): DevTerminatePlacementRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    terminationRequestedDate: (json.terminationRequestedDate != null) ? LocalDate.parseIso(json.terminationRequestedDate) : null
  }
}


export function deserializeJsonMockVtjDataset(json: JsonOf<MockVtjDataset>): MockVtjDataset {
  return {
    ...json,
    persons: json.persons.map(e => deserializeJsonMockVtjPerson(e))
  }
}


export function deserializeJsonMockVtjPerson(json: JsonOf<MockVtjPerson>): MockVtjPerson {
  return {
    ...json,
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null,
    restrictedDetails: (json.restrictedDetails != null) ? deserializeJsonRestrictedDetails(json.restrictedDetails) : null
  }
}


export function deserializeJsonPlacementPlan(json: JsonOf<PlacementPlan>): PlacementPlan {
  return {
    ...json,
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart),
    preschoolDaycarePeriodEnd: (json.preschoolDaycarePeriodEnd != null) ? LocalDate.parseIso(json.preschoolDaycarePeriodEnd) : null,
    preschoolDaycarePeriodStart: (json.preschoolDaycarePeriodStart != null) ? LocalDate.parseIso(json.preschoolDaycarePeriodStart) : null
  }
}


export function deserializeJsonReservationInsert(json: JsonOf<ReservationInsert>): ReservationInsert {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    range: (json.range != null) ? TimeRange.parseJson(json.range) : null
  }
}


export function deserializeJsonRestrictedDetails(json: JsonOf<RestrictedDetails>): RestrictedDetails {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null
  }
}


export function deserializeJsonVoucherValueDecision(json: JsonOf<VoucherValueDecision>): VoucherValueDecision {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    child: deserializeJsonChildWithDateOfBirth(json.child),
    created: HelsinkiDateTime.parseIso(json.created),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: LocalDate.parseIso(json.validTo)
  }
}
