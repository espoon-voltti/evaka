// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'
import { AbsenceCategory } from 'lib-common/generated/api-types/absence'
import { AbsenceType } from 'lib-common/generated/api-types/absence'
import { ApplicationForm } from 'lib-common/generated/api-types/application'
import { ApplicationOrigin } from 'lib-common/generated/api-types/application'
import { ApplicationStatus } from 'lib-common/generated/api-types/application'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { AssistanceLevel } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionEmployee } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionGuardian } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedPreschoolDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import { CalendarEventType } from 'lib-common/generated/api-types/calendarevent'
import { CareType } from 'lib-common/generated/api-types/daycare'
import { ChildWithDateOfBirth } from 'lib-common/generated/api-types/invoicing'
import { Coordinate } from 'lib-common/generated/api-types/shared'
import { CurriculumType } from 'lib-common/generated/api-types/vasu'
import { DailyServiceTimesType } from 'lib-common/generated/api-types/dailyservicetimes'
import { DaycareAssistanceLevel } from 'lib-common/generated/api-types/assistance'
import { DaycareDecisionCustomization } from 'lib-common/generated/api-types/daycare'
import { DecisionIncome } from 'lib-common/generated/api-types/invoicing'
import { DecisionStatus } from 'lib-common/generated/api-types/decision'
import { DecisionType } from 'lib-common/generated/api-types/decision'
import { DocumentContent } from 'lib-common/generated/api-types/document'
import { DocumentStatus } from 'lib-common/generated/api-types/document'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { DocumentType } from 'lib-common/generated/api-types/document'
import { FeeAlterationWithEffect } from 'lib-common/generated/api-types/invoicing'
import { FeeDecisionThresholds } from 'lib-common/generated/api-types/invoicing'
import { IncomeEffect } from 'lib-common/generated/api-types/invoicing'
import { IncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'
import { IncomeValue } from 'lib-common/generated/api-types/invoicing'
import { JsonOf } from 'lib-common/json'
import { Language } from 'lib-common/generated/api-types/daycare'
import { MailingAddress } from 'lib-common/generated/api-types/daycare'
import { Nationality } from 'lib-common/generated/api-types/vtjclient'
import { NativeLanguage } from 'lib-common/generated/api-types/vtjclient'
import { OfficialLanguage } from 'lib-common/generated/api-types/shared'
import { OtherAssistanceMeasureType } from 'lib-common/generated/api-types/assistance'
import { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import { PilotFeature } from 'lib-common/generated/api-types/shared'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { PreschoolAssistanceLevel } from 'lib-common/generated/api-types/assistance'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { PushNotificationCategory } from 'lib-common/generated/api-types/webpush'
import { ServiceOptions } from 'lib-common/generated/api-types/assistanceneed'
import { ShiftCareType } from 'lib-common/generated/api-types/serviceneed'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import { StructuralMotivationOptions } from 'lib-common/generated/api-types/assistanceneed'
import { UUID } from 'lib-common/types'
import { UnitManager } from 'lib-common/generated/api-types/daycare'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { VisitingAddress } from 'lib-common/generated/api-types/daycare'
import { VoucherValueDecisionDifference } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionServiceNeed } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionStatus } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonApplicationForm } from 'lib-common/generated/api-types/application'
import { deserializeJsonAssistanceNeedPreschoolDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonChildWithDateOfBirth } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonDocumentContent } from 'lib-common/generated/api-types/document'
import { deserializeJsonIncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'

/**
* Generated from fi.espoo.evaka.shared.dev.MockDigitransit.Autocomplete
*/
export interface Autocomplete {
  features: Feature[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.Caretaker
*/
export interface Caretaker {
  amount: number
  endDate: LocalDate | null
  groupId: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.Citizen
*/
export interface Citizen {
  dependantCount: number
  firstName: string
  lastName: string
  ssn: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.CreateVasuTemplateBody
*/
export interface CreateVasuTemplateBody {
  language: OfficialLanguage
  name: string
  type: CurriculumType
  valid: FiniteDateRange
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
  applicationId: UUID
  employeeId: UUID
  endDate: LocalDate
  id: UUID
  startDate: LocalDate
  status: DecisionStatus
  type: DecisionType
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAbsence
*/
export interface DevAbsence {
  absenceCategory: AbsenceCategory
  absenceType: AbsenceType
  childId: UUID
  date: LocalDate
  id: UUID
  modifiedAt: HelsinkiDateTime
  modifiedBy: UUID
  questionnaireId: UUID | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApplicationWithForm
*/
export interface DevApplicationWithForm {
  allowOtherGuardianAccess: boolean
  checkedByAdmin: boolean
  childId: UUID
  createdDate: HelsinkiDateTime | null
  dueDate: LocalDate | null
  form: ApplicationForm
  formModified: HelsinkiDateTime
  guardianId: UUID
  hideFromGuardian: boolean
  id: UUID
  modifiedDate: HelsinkiDateTime | null
  origin: ApplicationOrigin
  otherGuardians: UUID[]
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
  childId: UUID
  endDate: LocalDate
  id: UUID
  otherAction: string
  startDate: LocalDate
  updatedBy: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceActionOption
*/
export interface DevAssistanceActionOption {
  descriptionFi: string | null
  id: UUID
  nameFi: string
  value: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceFactor
*/
export interface DevAssistanceFactor {
  capacityFactor: number
  childId: UUID
  id: UUID
  modified: HelsinkiDateTime
  modifiedBy: UUID
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
*/
export interface DevAssistanceNeedDecision {
  annulmentReason: string
  assistanceLevels: AssistanceLevel[]
  careMotivation: string | null
  childId: UUID
  decisionMade: LocalDate | null
  decisionMaker: AssistanceNeedDecisionEmployee | null
  decisionNumber: number | null
  expertResponsibilities: string | null
  guardianInfo: AssistanceNeedDecisionGuardian[]
  guardiansHeardOn: LocalDate | null
  id: UUID
  language: OfficialLanguage
  motivationForDecision: string | null
  otherRepresentativeDetails: string | null
  otherRepresentativeHeard: boolean
  pedagogicalMotivation: string | null
  preparedBy1: AssistanceNeedDecisionEmployee | null
  preparedBy2: AssistanceNeedDecisionEmployee | null
  selectedUnit: UUID | null
  sentForDecision: LocalDate | null
  serviceOptions: ServiceOptions
  servicesMotivation: string | null
  status: AssistanceNeedDecisionStatus
  structuralMotivationDescription: string | null
  structuralMotivationOptions: StructuralMotivationOptions
  unreadGuardianIds: UUID[] | null
  validityPeriod: DateRange
  viewOfGuardians: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceNeedPreschoolDecision
*/
export interface DevAssistanceNeedPreschoolDecision {
  annulmentReason: string
  childId: UUID
  decisionMade: LocalDate | null
  decisionNumber: number
  form: AssistanceNeedPreschoolDecisionForm
  id: UUID
  sentForDecision: LocalDate | null
  status: AssistanceNeedDecisionStatus
  unreadGuardianIds: UUID[] | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevAssistanceNeedVoucherCoefficient
*/
export interface DevAssistanceNeedVoucherCoefficient {
  childId: UUID
  coefficient: number
  id: UUID
  validityPeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevBackupCare
*/
export interface DevBackupCare {
  childId: UUID
  groupId: UUID | null
  id: UUID
  period: FiniteDateRange
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevBackupPickup
*/
export interface DevBackupPickup {
  childId: UUID
  id: UUID
  name: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCalendarEvent
*/
export interface DevCalendarEvent {
  description: string
  eventType: CalendarEventType
  id: UUID
  modifiedAt: HelsinkiDateTime
  period: FiniteDateRange
  title: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCalendarEventAttendee
*/
export interface DevCalendarEventAttendee {
  calendarEventId: UUID
  childId: UUID | null
  groupId: UUID | null
  id: UUID
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCalendarEventTime
*/
export interface DevCalendarEventTime {
  calendarEventId: UUID
  childId: UUID | null
  date: LocalDate
  end: LocalTime
  id: UUID
  modifiedAt: HelsinkiDateTime
  modifiedBy: UUID
  start: LocalTime
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevCareArea
*/
export interface DevCareArea {
  areaCode: number | null
  id: UUID
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
  id: UUID
  languageAtHome: string
  languageAtHomeDetails: string
  mealTextureId: number | null
  medication: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevChildAttendance
*/
export interface DevChildAttendance {
  arrived: LocalTime
  childId: UUID
  date: LocalDate
  departed: LocalTime | null
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevChildDocument
*/
export interface DevChildDocument {
  childId: UUID
  content: DocumentContent
  contentModifiedAt: HelsinkiDateTime
  contentModifiedBy: UUID | null
  id: UUID
  modifiedAt: HelsinkiDateTime
  publishedAt: HelsinkiDateTime | null
  publishedContent: DocumentContent | null
  status: DocumentStatus
  templateId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevClubTerm
*/
export interface DevClubTerm {
  applicationPeriod: FiniteDateRange
  id: UUID
  term: FiniteDateRange
  termBreaks: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevCreateIncomeStatements
*/
export interface DevCreateIncomeStatements {
  data: IncomeStatementBody[]
  personId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDailyServiceTimeNotification
*/
export interface DevDailyServiceTimeNotification {
  dailyServiceTimeId: UUID
  dateFrom: LocalDate
  guardianId: UUID
  hasDeletedReservations: boolean
  id: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDailyServiceTimes
*/
export interface DevDailyServiceTimes {
  childId: UUID
  fridayTimes: TimeRange | null
  id: UUID
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
  enabledPilotFeatures: PilotFeature[]
  financeDecisionHandler: UUID | null
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
  operationTimes: (TimeRange | null)[]
  ophOrganizerOid: string | null
  ophUnitOid: string | null
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
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareAssistance
*/
export interface DevDaycareAssistance {
  childId: UUID
  id: UUID
  level: DaycareAssistanceLevel
  modified: HelsinkiDateTime
  modifiedBy: UUID
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareGroup
*/
export interface DevDaycareGroup {
  daycareId: UUID
  endDate: LocalDate | null
  id: UUID
  jamixCustomerNumber: number | null
  name: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareGroupAcl
*/
export interface DevDaycareGroupAcl {
  employeeId: UUID
  groupId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
*/
export interface DevDaycareGroupPlacement {
  daycareGroupId: UUID
  daycarePlacementId: UUID
  endDate: LocalDate
  id: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevDocumentTemplate
*/
export interface DevDocumentTemplate {
  confidential: boolean
  content: DocumentTemplateContent
  id: UUID
  language: OfficialLanguage
  legalBasis: string
  name: string
  published: boolean
  type: DocumentType
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
  id: UUID
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
  userId: UUID | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFamilyContact
*/
export interface DevFamilyContact {
  childId: UUID
  contactPersonId: UUID
  id: UUID
  priority: number
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFosterParent
*/
export interface DevFosterParent {
  childId: UUID
  id: UUID
  parentId: UUID
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevFridgeChild
*/
export interface DevFridgeChild {
  childId: UUID
  conflict: boolean
  endDate: LocalDate
  headOfChild: UUID
  id: UUID
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
  partnershipId: UUID
  personId: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevGuardian
*/
export interface DevGuardian {
  childId: UUID
  guardianId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevHoliday
*/
export interface DevHoliday {
  date: LocalDate
  description: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevIncome
*/
export interface DevIncome {
  data: Record<string, IncomeValue>
  effect: IncomeEffect
  id: UUID
  isEntrepreneur: boolean
  personId: UUID
  updatedAt: HelsinkiDateTime
  updatedBy: UUID
  validFrom: LocalDate
  validTo: LocalDate | null
  worksAtEcha: boolean
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevMobileDevice
*/
export interface DevMobileDevice {
  id: UUID
  longTermToken: UUID | null
  name: string
  pushNotificationCategories: PushNotificationCategory[]
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevOtherAssistanceMeasure
*/
export interface DevOtherAssistanceMeasure {
  childId: UUID
  id: UUID
  modified: HelsinkiDateTime
  modifiedBy: UUID
  type: OtherAssistanceMeasureType
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevParentship
*/
export interface DevParentship {
  childId: UUID
  createdAt: HelsinkiDateTime
  endDate: LocalDate
  headOfChildId: UUID
  id: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPayment
*/
export interface DevPayment {
  amount: number
  dueDate: LocalDate | null
  id: UUID
  number: number
  paymentDate: LocalDate | null
  period: FiniteDateRange
  sentAt: HelsinkiDateTime | null
  sentBy: UUID | null
  status: PaymentStatus
  unitBusinessId: string | null
  unitIban: string | null
  unitId: UUID
  unitName: string
  unitProviderId: string | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPedagogicalDocument
*/
export interface DevPedagogicalDocument {
  childId: UUID
  description: string
  id: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPerson
*/
export interface DevPerson {
  backupPhone: string
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  duplicateOf: UUID | null
  email: string | null
  enabledEmailTypes: EmailMessageType[] | null
  evakaUserId: UUID
  firstName: string
  forceManualFeeDecisions: boolean
  id: UUID
  invoiceRecipientName: string
  invoicingPostOffice: string
  invoicingPostalCode: string
  invoicingStreetAddress: string
  language: string | null
  lastName: string
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
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevPersonEmail
*/
export interface DevPersonEmail {
  email: string | null
  personId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPersonalMobileDevice
*/
export interface DevPersonalMobileDevice {
  employeeId: UUID
  id: UUID
  longTermToken: UUID | null
  name: string
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPlacement
*/
export interface DevPlacement {
  childId: UUID
  endDate: LocalDate
  id: UUID
  placeGuarantee: boolean
  startDate: LocalDate
  terminatedBy: UUID | null
  terminationRequestedDate: LocalDate | null
  type: PlacementType
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPreschoolAssistance
*/
export interface DevPreschoolAssistance {
  childId: UUID
  id: UUID
  level: PreschoolAssistanceLevel
  modified: HelsinkiDateTime
  modifiedBy: UUID
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevPreschoolTerm
*/
export interface DevPreschoolTerm {
  applicationPeriod: FiniteDateRange
  extendedTerm: FiniteDateRange
  finnishPreschool: FiniteDateRange
  id: UUID
  swedishPreschool: FiniteDateRange
  termBreaks: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevServiceNeed
*/
export interface DevServiceNeed {
  confirmedAt: HelsinkiDateTime | null
  confirmedBy: UUID
  endDate: LocalDate
  id: UUID
  optionId: UUID
  partWeek: boolean
  placementId: UUID
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
  employeeId: UUID
  groupId: UUID | null
  id: UUID
  occupancyCoefficient: number
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevStaffAttendancePlan
*/
export interface DevStaffAttendancePlan {
  description: string | null
  employeeId: UUID
  endTime: HelsinkiDateTime
  id: UUID
  startTime: HelsinkiDateTime
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevTerminatePlacementRequest
*/
export interface DevTerminatePlacementRequest {
  endDate: LocalDate
  placementId: UUID
  terminatedBy: UUID | null
  terminationRequestedDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevUpsertStaffOccupancyCoefficient
*/
export interface DevUpsertStaffOccupancyCoefficient {
  coefficient: number
  employeeId: UUID
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevVardaReset
*/
export interface DevVardaReset {
  evakaChildId: UUID
  resetTimestamp: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.DevVardaServiceNeed
*/
export interface DevVardaServiceNeed {
  errors: string[] | null
  evakaChildId: UUID
  evakaServiceNeedId: UUID
  evakaServiceNeedUpdated: HelsinkiDateTime
  updateFailed: boolean | null
}

/**
* Generated from fi.espoo.evaka.emailclient.Email
*/
export interface Email {
  content: EmailContent
  fromAddress: string
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
* Generated from fi.espoo.evaka.pis.EmailMessageType
*/
export type EmailMessageType =
  | 'TRANSACTIONAL'
  | 'MESSAGE_NOTIFICATION'
  | 'BULLETIN_NOTIFICATION'
  | 'OUTDATED_INCOME_NOTIFICATION'
  | 'NEW_CUSTOMER_INCOME_NOTIFICATION'
  | 'CALENDAR_EVENT_NOTIFICATION'
  | 'DECISION_NOTIFICATION'
  | 'DOCUMENT_NOTIFICATION'
  | 'INFORMAL_DOCUMENT_NOTIFICATION'
  | 'MISSING_ATTENDANCE_RESERVATION_NOTIFICATION'
  | 'MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION'

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
* Generated from fi.espoo.evaka.shared.dev.MockDigitransit.Geometry
*/
export interface Geometry {
  coordinates: [number, number]
}

/**
* Generated from fi.espoo.evaka.vtjclient.service.persondetails.MockVtjDataset
*/
export interface MockVtjDataset {
  guardianDependants: Record<string, string[]>
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
  nationalities: Nationality[]
  nativeLanguage: NativeLanguage | null
  residenceCode: string | null
  restrictedDetails: RestrictedDetails | null
  socialSecurityNumber: string
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
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.shared.dev.PostVasuDocBody
*/
export interface PostVasuDocBody {
  childId: UUID
  templateId: UUID
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
  messageId: string
  postOffice: string
  postalCode: string
  ssn: string
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecision
*/
export interface VoucherValueDecision {
  approvedAt: HelsinkiDateTime | null
  approvedById: UUID | null
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
  headOfFamilyId: UUID
  headOfFamilyIncome: DecisionIncome | null
  id: UUID
  partnerId: UUID | null
  partnerIncome: DecisionIncome | null
  placement: VoucherValueDecisionPlacement | null
  sentAt: HelsinkiDateTime | null
  serviceNeed: VoucherValueDecisionServiceNeed | null
  siblingDiscount: number
  status: VoucherValueDecisionStatus
  validFrom: LocalDate
  validTo: LocalDate | null
  voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
*/
export interface VoucherValueDecisionPlacement {
  type: PlacementType
  unitId: UUID
}


export function deserializeJsonCaretaker(json: JsonOf<Caretaker>): Caretaker {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonCreateVasuTemplateBody(json: JsonOf<CreateVasuTemplateBody>): CreateVasuTemplateBody {
  return {
    ...json,
    valid: FiniteDateRange.parseJson(json.valid)
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
    createdDate: (json.createdDate != null) ? HelsinkiDateTime.parseIso(json.createdDate) : null,
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    form: deserializeJsonApplicationForm(json.form),
    formModified: HelsinkiDateTime.parseIso(json.formModified),
    modifiedDate: (json.modifiedDate != null) ? HelsinkiDateTime.parseIso(json.modifiedDate) : null,
    sentDate: (json.sentDate != null) ? LocalDate.parseIso(json.sentDate) : null
  }
}


export function deserializeJsonDevAssistanceAction(json: JsonOf<DevAssistanceAction>): DevAssistanceAction {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDevAssistanceFactor(json: JsonOf<DevAssistanceFactor>): DevAssistanceFactor {
  return {
    ...json,
    modified: HelsinkiDateTime.parseIso(json.modified),
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
    validityPeriod: FiniteDateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonDevBackupCare(json: JsonOf<DevBackupCare>): DevBackupCare {
  return {
    ...json,
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
    departed: (json.departed != null) ? LocalTime.parseIso(json.departed) : null
  }
}


export function deserializeJsonDevChildDocument(json: JsonOf<DevChildDocument>): DevChildDocument {
  return {
    ...json,
    content: deserializeJsonDocumentContent(json.content),
    contentModifiedAt: HelsinkiDateTime.parseIso(json.contentModifiedAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    publishedContent: (json.publishedContent != null) ? deserializeJsonDocumentContent(json.publishedContent) : null
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


export function deserializeJsonDevCreateIncomeStatements(json: JsonOf<DevCreateIncomeStatements>): DevCreateIncomeStatements {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonIncomeStatementBody(e))
  }
}


export function deserializeJsonDevDailyServiceTimeNotification(json: JsonOf<DevDailyServiceTimeNotification>): DevDailyServiceTimeNotification {
  return {
    ...json,
    dateFrom: LocalDate.parseIso(json.dateFrom)
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


export function deserializeJsonDevHoliday(json: JsonOf<DevHoliday>): DevHoliday {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonDevIncome(json: JsonOf<DevIncome>): DevIncome {
  return {
    ...json,
    updatedAt: HelsinkiDateTime.parseIso(json.updatedAt),
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
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
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
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


export function deserializeJsonDevVardaReset(json: JsonOf<DevVardaReset>): DevVardaReset {
  return {
    ...json,
    resetTimestamp: (json.resetTimestamp != null) ? HelsinkiDateTime.parseIso(json.resetTimestamp) : null
  }
}


export function deserializeJsonDevVardaServiceNeed(json: JsonOf<DevVardaServiceNeed>): DevVardaServiceNeed {
  return {
    ...json,
    evakaServiceNeedUpdated: HelsinkiDateTime.parseIso(json.evakaServiceNeedUpdated)
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
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}
