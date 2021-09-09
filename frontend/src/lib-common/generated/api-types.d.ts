// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../types'
import LocalDate from '../local-date'
import FiniteDateRange from '../finite-date-range'
import DateRange from '../date-range'
import {DailyServiceTimes} from '../api-types/child/common'

/**
* Generated from fi.espoo.evaka.application.AcceptDecisionRequest
*/
export interface AcceptDecisionRequest {
    decisionId: UUID
    requestedStartDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.application.Address
*/
export interface Address {
    postOffice: string
    postalCode: string
    street: string
}

/**
* Generated from fi.espoo.evaka.application.ApplicationAttachment
*/
export interface ApplicationAttachment {
    contentType: string
    id: UUID
    name: string
    receivedAt: Date
    type: AttachmentType
    updated: Date
    uploadedByEmployee: UUID | null
    uploadedByPerson: UUID | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationDecisions
*/
export interface ApplicationDecisions {
    applicationId: UUID
    childName: string
    decisions: DecisionSummary[]
}

/**
* Generated from fi.espoo.evaka.application.ApplicationDetails
*/
export interface ApplicationDetails {
    additionalDaycareApplication: boolean
    attachments: ApplicationAttachment[]
    checkedByAdmin: boolean
    childId: UUID
    childRestricted: boolean
    createdDate: Date | null
    dueDate: LocalDate | null
    dueDateSetManuallyAt: Date | null
    form: ApplicationForm
    guardianDateOfDeath: LocalDate | null
    guardianId: UUID
    guardianRestricted: boolean
    hideFromGuardian: boolean
    id: UUID
    modifiedDate: Date | null
    origin: ApplicationOrigin
    otherGuardianId: UUID | null
    otherGuardianLivesInSameAddress: boolean | null
    sentDate: LocalDate | null
    status: ApplicationStatus
    transferApplication: boolean
    type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.ApplicationForm
*/
export interface ApplicationForm {
    child: ChildDetails
    clubDetails: ClubDetails | null
    guardian: Guardian
    maxFeeAccepted: boolean
    otherChildren: PersonBasics[]
    otherInfo: string
    otherPartner: PersonBasics | null
    preferences: Preferences
    secondGuardian: SecondGuardian | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationFormUpdate
*/
export interface ApplicationFormUpdate {
    child: ChildDetailsUpdate
    clubDetails: ClubDetails | null
    guardian: GuardianUpdate
    maxFeeAccepted: boolean
    otherChildren: PersonBasics[]
    otherInfo: string
    otherPartner: PersonBasics | null
    preferences: Preferences
    secondGuardian: SecondGuardian | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationOrigin
*/
export type ApplicationOrigin = 'ELECTRONIC' | 'PAPER'

/**
* Generated from fi.espoo.evaka.application.ApplicationResponse
*/
export interface ApplicationResponse {
    application: ApplicationDetails
    attachments: ApplicationAttachment[]
    decisions: Decision[]
    guardians: PersonJSON[]
}

/**
* Generated from fi.espoo.evaka.application.ApplicationSortColumn
*/
export type ApplicationSortColumn = 'APPLICATION_TYPE' | 'CHILD_NAME' | 'DUE_DATE' | 'START_DATE' | 'STATUS'

/**
* Generated from fi.espoo.evaka.application.ApplicationSortDirection
*/
export type ApplicationSortDirection = 'ASC' | 'DESC'

/**
* Generated from fi.espoo.evaka.application.ApplicationStatus
*/
export type ApplicationStatus = 'CREATED' | 'SENT' | 'WAITING_PLACEMENT' | 'WAITING_UNIT_CONFIRMATION' | 'WAITING_DECISION' | 'WAITING_MAILING' | 'WAITING_CONFIRMATION' | 'REJECTED' | 'ACTIVE' | 'CANCELLED'

/**
* Generated from fi.espoo.evaka.application.ApplicationSummary
*/
export interface ApplicationSummary {
    additionalDaycareApplication: boolean
    additionalInfo: boolean
    assistanceNeed: boolean
    attachmentCount: number
    checkedByAdmin: boolean
    currentPlacementUnit: PreferredUnit | null
    dateOfBirth: string | null
    dueDate: string | null
    duplicateApplication: boolean
    extendedCare: boolean
    firstName: string
    id: UUID
    lastName: string
    origin: ApplicationOrigin
    placementProposalStatus: PlacementProposalStatus | null
    placementProposalUnitName: string | null
    placementType: PlacementType
    preferredUnits: PreferredUnit[]
    serviceNeed: ServiceNeedOption | null
    siblingBasis: boolean
    socialSecurityNumber: string | null
    startDate: string | null
    status: ApplicationStatus
    transferApplication: boolean
    type: ApplicationType
    urgent: boolean
    wasOnClubCare: boolean | null
    wasOnDaycare: boolean | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationType
*/
export type ApplicationType = 'CLUB' | 'DAYCARE' | 'PRESCHOOL'

/**
* Generated from fi.espoo.evaka.application.ApplicationTypeToggle
*/
export type ApplicationTypeToggle = 'CLUB' | 'DAYCARE' | 'PRESCHOOL' | 'ALL'

/**
* Generated from fi.espoo.evaka.application.ApplicationUpdate
*/
export interface ApplicationUpdate {
    dueDate: LocalDate | null
    form: ApplicationFormUpdate
}

/**
* Generated from fi.espoo.evaka.application.ApplicationsOfChild
*/
export interface ApplicationsOfChild {
    applicationSummaries: CitizenApplicationSummary[]
    childId: UUID
    childName: string
}

/**
* Generated from fi.espoo.evaka.application.ChildDetails
*/
export interface ChildDetails {
    address: Address | null
    allergies: string
    assistanceDescription: string
    assistanceNeeded: boolean
    dateOfBirth: LocalDate | null
    diet: string
    futureAddress: FutureAddress | null
    language: string
    nationality: string
    person: PersonBasics
}

/**
* Generated from fi.espoo.evaka.application.ChildDetailsUpdate
*/
export interface ChildDetailsUpdate {
    allergies: string
    assistanceDescription: string
    assistanceNeeded: boolean
    diet: string
    futureAddress: FutureAddress | null
}

/**
* Generated from fi.espoo.evaka.application.ChildInfo
*/
export interface ChildInfo {
    firstName: string
    lastName: string
    ssn: string | null
}

/**
* Generated from fi.espoo.evaka.application.CitizenApplicationSummary
*/
export interface CitizenApplicationSummary {
    allPreferredUnitNames: string[]
    applicationId: UUID
    applicationStatus: ApplicationStatus
    childId: UUID
    childName: string | null
    createdDate: Date
    modifiedDate: Date
    preferredUnitName: string | null
    sentDate: LocalDate | null
    startDate: LocalDate | null
    transferApplication: boolean
    type: string
}

/**
* Generated from fi.espoo.evaka.application.ClubDetails
*/
export interface ClubDetails {
    wasOnClubCare: boolean
    wasOnDaycare: boolean
}

/**
* Generated from fi.espoo.evaka.application.CreateApplicationBody
*/
export interface CreateApplicationBody {
    childId: UUID
    type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.DaycarePlacementPlan
*/
export interface DaycarePlacementPlan {
    period: FiniteDateRange
    preschoolDaycarePeriod: FiniteDateRange | null
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.application.DecisionDraftJSON
*/
export interface DecisionDraftJSON {
    child: ChildInfo
    decisions: DecisionDraft[]
    guardian: GuardianInfo
    otherGuardian: GuardianInfo | null
    placementUnitName: string
    unit: DecisionUnit
}

/**
* Generated from fi.espoo.evaka.application.DecisionSummary
*/
export interface DecisionSummary {
    decisionId: UUID
    resolved: LocalDate | null
    sentDate: LocalDate
    status: DecisionStatus
    type: DecisionType
}

/**
* Generated from fi.espoo.evaka.application.FutureAddress
*/
export interface FutureAddress {
    movingDate: LocalDate | null
    postOffice: string
    postalCode: string
    street: string
}

/**
* Generated from fi.espoo.evaka.application.Guardian
*/
export interface Guardian {
    address: Address | null
    email: string
    futureAddress: FutureAddress | null
    person: PersonBasics
    phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.GuardianInfo
*/
export interface GuardianInfo {
    firstName: string
    id: UUID | null
    isVtjGuardian: boolean
    lastName: string
    ssn: string | null
}

/**
* Generated from fi.espoo.evaka.application.GuardianUpdate
*/
export interface GuardianUpdate {
    email: string
    futureAddress: FutureAddress | null
    phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.OtherGuardianAgreementStatus
*/
export type OtherGuardianAgreementStatus = 'AGREED' | 'NOT_AGREED' | 'RIGHT_TO_GET_NOTIFIED'

/**
* Generated from fi.espoo.evaka.application.PaperApplicationCreateRequest
*/
export interface PaperApplicationCreateRequest {
    childId: UUID
    guardianId: UUID | null
    guardianSsn: string | null
    guardianToBeCreated: CreatePersonBody | null
    hideFromGuardian: boolean
    sentDate: LocalDate
    transferApplication: boolean
    type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.PersonApplicationSummary
*/
export interface PersonApplicationSummary {
    applicationId: UUID
    childId: UUID
    childName: string | null
    childSsn: string | null
    connectedDaycare: boolean
    guardianId: UUID
    guardianName: string
    preferredStartDate: LocalDate | null
    preferredUnitId: UUID | null
    preferredUnitName: string | null
    preparatoryEducation: boolean
    sentDate: LocalDate | null
    status: ApplicationStatus
    type: string
}

/**
* Generated from fi.espoo.evaka.application.PersonBasics
*/
export interface PersonBasics {
    firstName: string
    lastName: string
    socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.application.PlacementProposalConfirmationUpdate
*/
export interface PlacementProposalConfirmationUpdate {
    otherReason: string | null
    reason: PlacementPlanRejectReason | null
    status: PlacementPlanConfirmationStatus
}

/**
* Generated from fi.espoo.evaka.application.PlacementProposalStatus
*/
export interface PlacementProposalStatus {
    unitConfirmationStatus: PlacementPlanConfirmationStatus
    unitRejectOtherReason: string | null
    unitRejectReason: PlacementPlanRejectReason | null
}

/**
* Generated from fi.espoo.evaka.application.Preferences
*/
export interface Preferences {
    preferredStartDate: LocalDate | null
    preferredUnits: PreferredUnit[]
    preparatory: boolean
    serviceNeed: ServiceNeed | null
    siblingBasis: SiblingBasis | null
    urgent: boolean
}

/**
* Generated from fi.espoo.evaka.application.PreferredUnit
*/
export interface PreferredUnit {
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.application.RejectDecisionRequest
*/
export interface RejectDecisionRequest {
    decisionId: UUID
}

/**
* Generated from fi.espoo.evaka.application.SearchApplicationRequest
*/
export interface SearchApplicationRequest {
    area: string | null
    basis: string | null
    dateType: string | null
    distinctions: string | null
    page: number | null
    pageSize: number | null
    periodEnd: LocalDate | null
    periodStart: LocalDate | null
    preschoolType: string | null
    searchTerms: string | null
    sortBy: ApplicationSortColumn | null
    sortDir: ApplicationSortDirection | null
    status: string | null
    transferApplications: TransferApplicationFilter | null
    type: ApplicationTypeToggle
    units: string | null
    voucherApplications: VoucherApplicationFilter | null
}

/**
* Generated from fi.espoo.evaka.application.SecondGuardian
*/
export interface SecondGuardian {
    agreementStatus: OtherGuardianAgreementStatus | null
    email: string
    phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.ServiceNeed
*/
export interface ServiceNeed {
    endTime: string
    partTime: boolean
    serviceNeedOption: ServiceNeedOption | null
    shiftCare: boolean
    startTime: string
}

/**
* Generated from fi.espoo.evaka.application.ServiceNeedOption
*/
export interface ServiceNeedOption {
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.application.SiblingBasis
*/
export interface SiblingBasis {
    siblingName: string
    siblingSsn: string
}

/**
* Generated from fi.espoo.evaka.application.SimpleBatchRequest
*/
export interface SimpleBatchRequest {
    applicationIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.application.TransferApplicationFilter
*/
export type TransferApplicationFilter = 'TRANSFER_ONLY' | 'NO_TRANSFER' | 'ALL'

/**
* Generated from fi.espoo.evaka.application.VoucherApplicationFilter
*/
export type VoucherApplicationFilter = 'VOUCHER_FIRST_CHOICE' | 'VOUCHER_ONLY' | 'NO_VOUCHER'

/**
* Generated from fi.espoo.evaka.application.notes.NoteJSON
*/
export interface NoteJSON {
    applicationId: UUID
    created: Date
    createdBy: UUID
    createdByName: string
    id: UUID
    text: string
    updated: Date
    updatedBy: UUID | null
    updatedByName: string | null
}

/**
* Generated from fi.espoo.evaka.application.notes.NoteRequest
*/
export interface NoteRequest {
    text: string
}

/**
* Generated from fi.espoo.evaka.application.notes.NoteSearchDTO
*/
export interface NoteSearchDTO {
    applicationIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.application.notes.NotesWrapperJSON
*/
export interface NotesWrapperJSON {
    notes: NoteJSON[]
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceAction
*/
export interface AssistanceAction {
    actions: string[]
    childId: UUID
    endDate: LocalDate
    id: UUID
    measures: AssistanceMeasure[]
    otherAction: string
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceActionOption
*/
export interface AssistanceActionOption {
    nameFi: string
    value: string
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceActionRequest
*/
export interface AssistanceActionRequest {
    actions: string[]
    endDate: LocalDate
    measures: AssistanceMeasure[]
    otherAction: string
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceMeasure
*/
export type AssistanceMeasure = 'SPECIAL_ASSISTANCE_DECISION' | 'INTENSIFIED_ASSISTANCE' | 'EXTENDED_COMPULSORY_EDUCATION' | 'CHILD_SERVICE' | 'CHILD_ACCULTURATION_SUPPORT' | 'TRANSPORT_BENEFIT'

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceBasisOption
*/
export interface AssistanceBasisOption {
    descriptionFi: string | null
    nameFi: string
    value: string
}

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeed
*/
export interface AssistanceNeed {
    bases: string[]
    capacityFactor: number
    childId: UUID
    description: string
    endDate: LocalDate
    id: UUID
    otherBasis: string
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeedRequest
*/
export interface AssistanceNeedRequest {
    bases: string[]
    capacityFactor: number
    description: string
    endDate: LocalDate
    otherBasis: string
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.attachment.AttachmentType
*/
export type AttachmentType = 'URGENCY' | 'EXTENDED_CARE'

/**
* Generated from fi.espoo.evaka.attendance.AttendanceReservation
*/
export interface AttendanceReservation {
    endTime: string
    startTime: string
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceResponse
*/
export interface AttendanceResponse {
    children: Child[]
    unit: UnitInfo
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceStatus
*/
export type AttendanceStatus = 'COMING' | 'PRESENT' | 'DEPARTED' | 'ABSENT'

/**
* Generated from fi.espoo.evaka.attendance.Child
*/
export interface Child {
    absences: ChildAbsence[]
    attendance: ChildAttendance | null
    backup: boolean
    dailyNote: DaycareDailyNote | null
    dailyServiceTimes: DailyServiceTimes | null
    firstName: string
    groupId: UUID
    id: UUID
    imageUrl: string | null
    lastName: string
    placementType: PlacementType
    preferredName: string | null
    reservation: AttendanceReservation | null
    status: AttendanceStatus
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAbsence
*/
export interface ChildAbsence {
    careType: AbsenceCareType
    childId: UUID
    id: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendance
*/
export interface ChildAttendance {
    arrived: Date
    childId: UUID
    departed: Date | null
    id: UUID
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.AbsenceRangeRequest
*/
export interface AbsenceRangeRequest {
    absenceType: AbsenceType
    endDate: LocalDate
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ArrivalRequest
*/
export interface ArrivalRequest {
    arrived: string
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DepartureInfoResponse
*/
export interface DepartureInfoResponse {
    absentFrom: AbsenceCareType[]
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DepartureRequest
*/
export interface DepartureRequest {
    absenceType: AbsenceType | null
    departed: string
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.FullDayAbsenceRequest
*/
export interface FullDayAbsenceRequest {
    absenceType: AbsenceType
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.GetChildSensitiveInfoRequest
*/
export interface GetChildSensitiveInfoRequest {
    pin: string
    staffId: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.ChildResult
*/
export interface ChildResult {
    child: ChildSensitiveInformation | null
    status: ChildResultStatus
}

/**
* Generated from fi.espoo.evaka.attendance.ChildResultStatus
*/
export type ChildResultStatus = 'SUCCESS' | 'WRONG_PIN' | 'PIN_LOCKED' | 'NOT_FOUND'

/**
* Generated from fi.espoo.evaka.attendance.ChildSensitiveInformation
*/
export interface ChildSensitiveInformation {
    allergies: string
    backupPickups: ContactInfo[]
    childAddress: string
    contacts: ContactInfo[]
    diet: string
    firstName: string
    id: UUID
    lastName: string
    medication: string
    placementTypes: PlacementType[]
    preferredName: string
    ssn: string
}

/**
* Generated from fi.espoo.evaka.attendance.ContactInfo
*/
export interface ContactInfo {
    backupPhone: string
    email: string
    firstName: string
    id: string
    lastName: string
    phone: string
    priority: number | null
}

/**
* Generated from fi.espoo.evaka.attendance.GroupInfo
*/
export interface GroupInfo {
    dailyNote: DaycareDailyNote | null
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.attendance.Staff
*/
export interface Staff {
    firstName: string
    groups: UUID[]
    id: UUID
    lastName: string
    pinSet: boolean
}

/**
* Generated from fi.espoo.evaka.attendance.UnitInfo
*/
export interface UnitInfo {
    groups: GroupInfo[]
    id: UUID
    name: string
    staff: Staff[]
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareChild
*/
export interface BackupCareChild {
    birthDate: LocalDate
    firstName: string
    id: UUID
    lastName: string
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareCreateResponse
*/
export interface BackupCareCreateResponse {
    id: UUID
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareGroup
*/
export interface BackupCareGroup {
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareUnit
*/
export interface BackupCareUnit {
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareUpdateRequest
*/
export interface BackupCareUpdateRequest {
    groupId: UUID | null
    period: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.backupcare.ChildBackupCare
*/
export interface ChildBackupCare {
    group: BackupCareGroup | null
    id: UUID
    period: FiniteDateRange
    unit: BackupCareUnit
}

/**
* Generated from fi.espoo.evaka.backupcare.ChildBackupCaresResponse
*/
export interface ChildBackupCaresResponse {
    backupCares: ChildBackupCare[]
}

/**
* Generated from fi.espoo.evaka.backupcare.NewBackupCare
*/
export interface NewBackupCare {
    groupId: UUID | null
    period: FiniteDateRange
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.backupcare.UnitBackupCare
*/
export interface UnitBackupCare {
    child: BackupCareChild
    group: BackupCareGroup | null
    id: UUID
    missingServiceNeedDays: number
    period: FiniteDateRange
    serviceNeeds: ServiceNeed[]
}

/**
* Generated from fi.espoo.evaka.backupcare.UnitBackupCaresResponse
*/
export interface UnitBackupCaresResponse {
    backupCares: UnitBackupCare[]
}

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickup
*/
export interface ChildBackupPickup {
    childId: UUID
    id: UUID
    name: string
    phone: string
}

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickupContent
*/
export interface ChildBackupPickupContent {
    name: string
    phone: string
}

/**
* Generated from fi.espoo.evaka.backuppickup.ChildBackupPickupCreateResponse
*/
export interface ChildBackupPickupCreateResponse {
    id: UUID
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.DailyServiceTimesResponse
*/
export interface DailyServiceTimesResponse {
    dailyServiceTimes: DailyServiceTimes | null
}

/**
* Generated from fi.espoo.evaka.daycare.CareType
*/
export type CareType = 'CLUB' | 'FAMILY' | 'CENTRE' | 'GROUP_FAMILY' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION'

/**
* Generated from fi.espoo.evaka.daycare.ClubTerm
*/
export interface ClubTerm {
    applicationPeriod: FiniteDateRange
    term: FiniteDateRange
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
    ophOrganizationOid: string | null
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
    ophOrganizationOid: string | null
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
* Generated from fi.espoo.evaka.daycare.FinanceDecisionHandler
*/
export interface FinanceDecisionHandler {
    LastName: string
    firstName: string
    id: UUID
}

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
* Generated from fi.espoo.evaka.daycare.UnitManager
*/
export interface UnitManager {
    email: string | null
    name: string | null
    phone: string | null
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
* Generated from fi.espoo.evaka.daycare.controllers.DaycareAclResponse
*/
export interface DaycareAclResponse {
    rows: DaycareAclRow[]
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
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.GroupUpdateRequest
*/
export interface GroupUpdateRequest {
    endDate: LocalDate | null
    name: string
    startDate: LocalDate
}

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
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.GroupAclUpdate
*/
export interface GroupAclUpdate {
    groupIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.daycare.domain.Language
*/
export type Language = 'fi' | 'sv' | 'en'

/**
* Generated from fi.espoo.evaka.daycare.domain.ProviderType
*/
export type ProviderType = 'MUNICIPAL' | 'PURCHASED' | 'PRIVATE' | 'MUNICIPAL_SCHOOL' | 'PRIVATE_SERVICE_VOUCHER'

/**
* Generated from fi.espoo.evaka.daycare.service.Absence
*/
export interface Absence {
    absenceType: AbsenceType
    careType: AbsenceCareType
    childId: UUID
    date: LocalDate
    id: UUID | null
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceCareType
*/
export type AbsenceCareType = 'SCHOOL_SHIFT_CARE' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE' | 'DAYCARE_5YO_FREE' | 'DAYCARE' | 'CLUB'

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceDelete
*/
export interface AbsenceDelete {
    careType: AbsenceCareType
    childId: UUID
    date: LocalDate
}

/**
* Generated from fi.espoo.evaka.daycare.service.AbsenceType
*/
export type AbsenceType = 'OTHER_ABSENCE' | 'SICKLEAVE' | 'UNKNOWN_ABSENCE' | 'PLANNED_ABSENCE' | 'TEMPORARY_RELOCATION' | 'TEMPORARY_VISITOR' | 'PARENTLEAVE' | 'FORCE_MAJEURE'

/**
* Generated from fi.espoo.evaka.daycare.service.CaretakerAmount
*/
export interface CaretakerAmount {
    amount: number
    endDate: LocalDate | null
    groupId: UUID
    id: UUID
    startDate: LocalDate
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
* Generated from fi.espoo.evaka.daycare.service.StaffAttendanceUpdate
*/
export interface StaffAttendanceUpdate {
    count: number | null
    countOther: number | null
    date: LocalDate
    groupId: UUID
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
* Generated from fi.espoo.evaka.decision.Decision
*/
export interface Decision {
    applicationId: UUID
    childId: UUID
    childName: string
    createdBy: string
    decisionNumber: number
    documentKey: string | null
    endDate: LocalDate
    id: UUID
    otherGuardianDocumentKey: string | null
    requestedStartDate: LocalDate | null
    resolved: LocalDate | null
    sentDate: LocalDate
    startDate: LocalDate
    status: DecisionStatus
    type: DecisionType
    unit: DecisionUnit
}

/**
* Generated from fi.espoo.evaka.decision.DecisionDraft
*/
export interface DecisionDraft {
    endDate: LocalDate
    id: UUID
    planned: boolean
    startDate: LocalDate
    type: DecisionType
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.decision.DecisionDraftService.DecisionDraftUpdate
*/
export interface DecisionDraftUpdate {
    endDate: LocalDate
    id: UUID
    planned: boolean
    startDate: LocalDate
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.decision.DecisionListResponse
*/
export interface DecisionListResponse {
    decisions: Decision[]
}

/**
* Generated from fi.espoo.evaka.decision.DecisionStatus
*/
export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

/**
* Generated from fi.espoo.evaka.decision.DecisionType
*/
export type DecisionType = 'CLUB' | 'DAYCARE' | 'DAYCARE_PART_TIME' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE' | 'PREPARATORY_EDUCATION'

/**
* Generated from fi.espoo.evaka.decision.DecisionUnit
*/
export interface DecisionUnit {
    daycareDecisionName: string
    decisionHandler: string
    decisionHandlerAddress: string
    id: UUID
    manager: string | null
    name: string
    phone: string | null
    postOffice: string
    postalCode: string
    preschoolDecisionName: string
    providerType: ProviderType
    streetAddress: string
}

/**
* Generated from fi.espoo.evaka.identity.ExternalId
*/
export interface ExternalId {
    namespace: string
    value: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAwaitingHandler
*/
export interface IncomeStatementAwaitingHandler {
    id: UUID
    personId: UUID
    personName: string
    type: IncomeStatementType
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementType
*/
export type IncomeStatementType = 'HIGHEST_FEE' | 'INCOME'

/**
* Generated from fi.espoo.evaka.invoicing.controller.CreateRetroactiveFeeDecisionsBody
*/
export interface CreateRetroactiveFeeDecisionsBody {
    from: LocalDate
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
*/
export type FeeDecisionSortParam = 'HEAD_OF_FAMILY' | 'VALIDITY' | 'NUMBER' | 'CREATED' | 'SENT' | 'STATUS' | 'FINAL_PRICE'

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionTypeRequest
*/
export interface FeeDecisionTypeRequest {
    type: FeeDecisionType
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeThresholdsWithId
*/
export interface FeeThresholdsWithId {
    id: UUID
    thresholds: FeeThresholds
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.GenerateDecisionsBody
*/
export interface GenerateDecisionsBody {
    starting: string
    targetHeads: UUID[]
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoicePayload
*/
export interface InvoicePayload {
    areas: string[]
    dueDate: LocalDate | null
    from: LocalDate
    invoiceDate: LocalDate | null
    to: LocalDate
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceSortParam
*/
export type InvoiceSortParam = 'HEAD_OF_FAMILY' | 'CHILDREN' | 'START' | 'END' | 'SUM' | 'STATUS' | 'CREATED_AT'

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchFeeDecisionRequest
*/
export interface SearchFeeDecisionRequest {
    area: string | null
    distinctions: string | null
    endDate: string | null
    financeDecisionHandlerId: UUID | null
    page: number
    pageSize: number
    searchByStartDate: boolean
    searchTerms: string | null
    sortBy: FeeDecisionSortParam | null
    sortDirection: SortDirection | null
    startDate: string | null
    status: string | null
    unit: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchInvoicesRequest
*/
export interface SearchInvoicesRequest {
    area: string | null
    distinctions: string | null
    page: number
    pageSize: number
    periodEnd: string | null
    periodStart: string | null
    searchTerms: string | null
    sortBy: InvoiceSortParam | null
    sortDirection: SortDirection | null
    status: string | null
    unit: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchVoucherValueDecisionRequest
*/
export interface SearchVoucherValueDecisionRequest {
    area: string | null
    endDate: string | null
    financeDecisionHandlerId: UUID | null
    page: number
    pageSize: number
    searchByStartDate: boolean
    searchTerms: string | null
    sortBy: VoucherValueDecisionSortParam | null
    sortDirection: SortDirection | null
    startDate: string | null
    status: string | null
    unit: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SortDirection
*/
export type SortDirection = 'ASC' | 'DESC'

/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
*/
export type VoucherValueDecisionSortParam = 'HEAD_OF_FAMILY' | 'STATUS'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlteration
*/
export interface FeeAlteration {
    amount: number
    id: UUID | null
    isAbsolute: boolean
    notes: string
    personId: UUID
    type: Type
    updatedAt: Date | null
    updatedBy: UUID | null
    validFrom: LocalDate
    validTo: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlteration.Type
*/
export type Type = 'DISCOUNT' | 'INCREASE' | 'RELIEF'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
*/
export type FeeDecisionStatus = 'DRAFT' | 'WAITING_FOR_SENDING' | 'WAITING_FOR_MANUAL_SENDING' | 'SENT' | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
*/
export interface FeeDecisionSummary {
    approvedAt: Date | null
    children: Basic[]
    created: Date
    decisionNumber: number | null
    finalPrice: number
    headOfFamily: Basic
    id: UUID
    sentAt: Date | null
    status: FeeDecisionStatus
    validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionType
*/
export type FeeDecisionType = 'NORMAL' | 'RELIEF_REJECTED' | 'RELIEF_PARTLY_ACCEPTED' | 'RELIEF_ACCEPTED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeThresholds
*/
export interface FeeThresholds {
    incomeMultiplier2: number
    incomeMultiplier3: number
    incomeMultiplier4: number
    incomeMultiplier5: number
    incomeMultiplier6: number
    incomeThresholdIncrease6Plus: number
    maxFee: number
    maxIncomeThreshold2: number
    maxIncomeThreshold3: number
    maxIncomeThreshold4: number
    maxIncomeThreshold5: number
    maxIncomeThreshold6: number
    minFee: number
    minIncomeThreshold2: number
    minIncomeThreshold3: number
    minIncomeThreshold4: number
    minIncomeThreshold5: number
    minIncomeThreshold6: number
    siblingDiscount2: number
    siblingDiscount2Plus: number
    validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.IncomeEffect
*/
export type IncomeEffect = 'MAX_FEE_ACCEPTED' | 'INCOMPLETE' | 'INCOME' | 'NOT_AVAILABLE'

/**
* Generated from fi.espoo.evaka.invoicing.domain.Invoice
*/
export interface Invoice {
    agreementType: number
    dueDate: LocalDate
    headOfFamily: JustId
    id: UUID
    invoiceDate: LocalDate
    number: number | null
    periodEnd: LocalDate
    periodStart: LocalDate
    rows: InvoiceRow[]
    sentAt: Date | null
    sentBy: UUID | null
    status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceDetailed
*/
export interface InvoiceDetailed {
    account: number
    agreementType: number
    dueDate: LocalDate
    headOfFamily: Detailed
    id: UUID
    invoiceDate: LocalDate
    number: number | null
    periodEnd: LocalDate
    periodStart: LocalDate
    rows: InvoiceRowDetailed[]
    sentAt: Date | null
    sentBy: UUID | null
    status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRow
*/
export interface InvoiceRow {
    amount: number
    child: WithDateOfBirth
    costCenter: string
    description: string
    id: UUID | null
    periodEnd: LocalDate
    periodStart: LocalDate
    product: Product
    subCostCenter: string | null
    unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
*/
export interface InvoiceRowDetailed {
    amount: number
    child: Detailed
    costCenter: string
    description: string
    id: UUID
    periodEnd: LocalDate
    periodStart: LocalDate
    product: Product
    subCostCenter: string | null
    unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
*/
export interface InvoiceRowSummary {
    amount: number
    child: Basic
    id: UUID
    unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceStatus
*/
export type InvoiceStatus = 'DRAFT' | 'WAITING_FOR_SENDING' | 'SENT' | 'CANCELED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceSummary
*/
export interface InvoiceSummary {
    account: number
    createdAt: Date | null
    headOfFamily: Detailed
    id: UUID
    periodEnd: LocalDate
    periodStart: LocalDate
    rows: InvoiceRowSummary[]
    sentAt: Date | null
    sentBy: UUID | null
    status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.Basic
*/
export interface Basic {
    dateOfBirth: LocalDate
    firstName: string
    id: UUID
    lastName: string
    ssn: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.Detailed
*/
export interface Detailed {
    dateOfBirth: LocalDate
    dateOfDeath: LocalDate | null
    email: string | null
    firstName: string
    forceManualFeeDecisions: boolean
    id: UUID
    invoiceRecipientName: string
    invoicingPostOffice: string
    invoicingPostalCode: string
    invoicingStreetAddress: string
    language: string | null
    lastName: string
    phone: string | null
    postOffice: string | null
    postalCode: string | null
    residenceCode: string | null
    restrictedDetailsEnabled: boolean
    ssn: string | null
    streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.JustId
*/
export interface JustId {
    id: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.WithDateOfBirth
*/
export interface WithDateOfBirth {
    dateOfBirth: LocalDate
    id: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.Product
*/
export type Product = 'DAYCARE' | 'DAYCARE_DISCOUNT' | 'DAYCARE_INCREASE' | 'PRESCHOOL_WITH_DAYCARE' | 'PRESCHOOL_WITH_DAYCARE_DISCOUNT' | 'PRESCHOOL_WITH_DAYCARE_INCREASE' | 'TEMPORARY_CARE' | 'SCHOOL_SHIFT_CARE' | 'SICK_LEAVE_100' | 'SICK_LEAVE_50' | 'ABSENCE' | 'FREE_OF_CHARGE'

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
*/
export type VoucherValueDecisionStatus = 'DRAFT' | 'WAITING_FOR_SENDING' | 'WAITING_FOR_MANUAL_SENDING' | 'SENT' | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
*/
export interface VoucherValueDecisionSummary {
    approvedAt: Date | null
    child: Basic
    created: Date
    decisionNumber: number | null
    finalCoPayment: number
    headOfFamily: Basic
    id: UUID
    sentAt: Date | null
    status: VoucherValueDecisionStatus
    validFrom: LocalDate
    validTo: LocalDate | null
    voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.service.InvoiceCodes
*/
export interface InvoiceCodes {
    agreementTypes: number[]
    costCenters: string[]
    products: Product[]
    subCostCenters: string[]
}

/**
* Generated from fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNote
*/
export interface DaycareDailyNote {
    childId: UUID | null
    date: LocalDate
    feedingNote: DaycareDailyNoteLevelInfo | null
    groupId: UUID | null
    id: UUID | null
    modifiedAt: Date | null
    modifiedBy: string | null
    note: string | null
    reminderNote: string | null
    reminders: DaycareDailyNoteReminder[]
    sleepingMinutes: number | null
    sleepingNote: DaycareDailyNoteLevelInfo | null
}

/**
* Generated from fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNoteLevelInfo
*/
export type DaycareDailyNoteLevelInfo = 'GOOD' | 'MEDIUM' | 'NONE'

/**
* Generated from fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNoteReminder
*/
export type DaycareDailyNoteReminder = 'DIAPERS' | 'CLOTHES' | 'LAUNDRY'

/**
* Generated from fi.espoo.evaka.messaging.message.AccountType
*/
export type AccountType = 'PERSONAL' | 'GROUP'

/**
* Generated from fi.espoo.evaka.messaging.message.ChildRecipientsController.EditRecipientRequest
*/
export interface EditRecipientRequest {
    blocklisted: boolean
}

/**
* Generated from fi.espoo.evaka.messaging.message.CitizenMessageBody
*/
export interface CitizenMessageBody {
    content: string
    recipients: MessageAccount[]
    title: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.DetailedMessageAccount
*/
export interface DetailedMessageAccount {
    daycareGroup: Group | null
    id: UUID
    name: string
    type: AccountType
    unreadCount: number
}

/**
* Generated from fi.espoo.evaka.messaging.message.DraftContent
*/
export interface DraftContent {
    content: string
    created: Date
    id: UUID
    recipientIds: UUID[]
    recipientNames: string[]
    title: string
    type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.message.Group
*/
export interface Group {
    id: UUID
    name: string
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.Message
*/
export interface Message {
    content: string
    id: UUID
    readAt: Date | null
    recipients: MessageAccount[]
    senderId: UUID
    senderName: string
    sentAt: Date
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageAccount
*/
export interface MessageAccount {
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageController.PostMessageBody
*/
export interface PostMessageBody {
    content: string
    recipientAccountIds: UUID[]
    recipientNames: string[]
    title: string
    type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageController.ReplyToMessageBody
*/
export interface ReplyToMessageBody {
    content: string
    recipientAccountIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageControllerCitizen.ReplyToMessageBody
*/
export interface ReplyToMessageBody {
    content: string
    recipientAccountIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageReceiver
*/
export interface MessageReceiver {
    childDateOfBirth: LocalDate
    childFirstName: string
    childId: UUID
    childLastName: string
    receiverPersons: MessageReceiverPerson[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageReceiverPerson
*/
export interface MessageReceiverPerson {
    accountId: UUID
    receiverFirstName: string
    receiverLastName: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageReceiversResponse
*/
export interface MessageReceiversResponse {
    groupId: UUID
    groupName: string
    receivers: MessageReceiver[]
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageService.ThreadReply
*/
export interface ThreadReply {
    message: Message
    threadId: UUID
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageThread
*/
export interface MessageThread {
    id: UUID
    messages: Message[]
    title: string
    type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.message.MessageType
*/
export type MessageType = 'MESSAGE' | 'BULLETIN'

/**
* Generated from fi.espoo.evaka.messaging.message.Recipient
*/
export interface Recipient {
    blocklisted: boolean
    firstName: string
    guardian: boolean
    headOfChild: boolean
    lastName: string
    personId: string
}

/**
* Generated from fi.espoo.evaka.messaging.message.SentMessage
*/
export interface SentMessage {
    content: string
    contentId: UUID
    recipientNames: string[]
    recipients: MessageAccount[]
    sentAt: Date
    threadTitle: string
    type: MessageType
}

/**
* Generated from fi.espoo.evaka.messaging.message.UnreadMessagesResponse
*/
export interface UnreadMessagesResponse {
    count: number
}

/**
* Generated from fi.espoo.evaka.messaging.message.UpsertableDraftContent
*/
export interface UpsertableDraftContent {
    content: string
    recipientIds: UUID[]
    recipientNames: string[]
    title: string
    type: MessageType
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyPeriod
*/
export interface OccupancyPeriod {
    caretakers: number | null
    headcount: number
    percentage: number | null
    period: FiniteDateRange
    sum: number
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyResponse
*/
export interface OccupancyResponse {
    max: OccupancyPeriod | null
    min: OccupancyPeriod | null
    occupancies: OccupancyPeriod[]
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyResponseGroupLevel
*/
export interface OccupancyResponseGroupLevel {
    groupId: UUID
    occupancies: OccupancyResponse
}

/**
* Generated from fi.espoo.evaka.pairing.MobileDevice
*/
export interface MobileDevice {
    id: UUID
    name: string
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.pairing.MobileDeviceIdentity
*/
export interface MobileDeviceIdentity {
    id: UUID
    longTermToken: UUID
}

/**
* Generated from fi.espoo.evaka.pairing.MobileDevicesController.RenameRequest
*/
export interface RenameRequest {
    name: string
}

/**
* Generated from fi.espoo.evaka.pairing.Pairing
*/
export interface Pairing {
    challengeKey: string
    expires: Date
    id: UUID
    mobileDeviceId: UUID | null
    responseKey: string | null
    status: PairingStatus
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.pairing.PairingStatus
*/
export type PairingStatus = 'WAITING_CHALLENGE' | 'WAITING_RESPONSE' | 'READY' | 'PAIRED'

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PairingStatusRes
*/
export interface PairingStatusRes {
    status: PairingStatus
}

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingChallengeReq
*/
export interface PostPairingChallengeReq {
    challengeKey: string
}

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingReq
*/
export interface PostPairingReq {
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingResponseReq
*/
export interface PostPairingResponseReq {
    challengeKey: string
    responseKey: string
}

/**
* Generated from fi.espoo.evaka.pairing.PairingsController.PostPairingValidationReq
*/
export interface PostPairingValidationReq {
    challengeKey: string
    responseKey: string
}

/**
* Generated from fi.espoo.evaka.pis.DaycareRole
*/
export interface DaycareRole {
    daycareId: UUID
    daycareName: string
    role: UserRole
}

/**
* Generated from fi.espoo.evaka.pis.Employee
*/
export interface Employee {
    created: Date
    email: string | null
    externalId: ExternalId | null
    firstName: string
    id: UUID
    lastName: string
    updated: Date | null
}

/**
* Generated from fi.espoo.evaka.pis.EmployeeUser
*/
export interface EmployeeUser {
    allScopedRoles: UserRole[]
    firstName: string
    globalRoles: UserRole[]
    id: UUID
    lastName: string
}

/**
* Generated from fi.espoo.evaka.pis.EmployeeWithDaycareRoles
*/
export interface EmployeeWithDaycareRoles {
    created: Date
    daycareRoles: DaycareRole[]
    email: string | null
    firstName: string
    globalRoles: UserRole[]
    id: UUID
    lastName: string
    updated: Date | null
}

/**
* Generated from fi.espoo.evaka.pis.FamilyContact
*/
export interface FamilyContact {
    backupPhone: string | null
    email: string | null
    firstName: string | null
    id: UUID
    lastName: string | null
    phone: string | null
    postOffice: string
    postalCode: string
    priority: number | null
    role: FamilyContactRole
    streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.FamilyContactRole
*/
export type FamilyContactRole = 'LOCAL_GUARDIAN' | 'LOCAL_ADULT' | 'LOCAL_SIBLING' | 'REMOTE_GUARDIAN'

/**
* Generated from fi.espoo.evaka.pis.NewEmployee
*/
export interface NewEmployee {
    email: string | null
    externalId: ExternalId | null
    firstName: string
    lastName: string
    roles: UserRole[]
}

/**
* Generated from fi.espoo.evaka.pis.SystemIdentityController.EmployeeIdentityRequest
*/
export interface EmployeeIdentityRequest {
    email: string | null
    externalId: ExternalId
    firstName: string
    lastName: string
}

/**
* Generated from fi.espoo.evaka.pis.SystemIdentityController.EmployeeUserResponse
*/
export interface EmployeeUserResponse {
    accessibleFeatures: EmployeeFeatures
    allScopedRoles: UserRole[]
    firstName: string
    globalRoles: UserRole[]
    id: UUID
    lastName: string
}

/**
* Generated from fi.espoo.evaka.pis.SystemIdentityController.PersonIdentityRequest
*/
export interface PersonIdentityRequest {
    firstName: string
    lastName: string
    socialSecurityNumber: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.CreatePersonBody
*/
export interface CreatePersonBody {
    dateOfBirth: LocalDate
    email: string | null
    firstName: string
    lastName: string
    phone: string | null
    postOffice: string
    postalCode: string
    streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.EmployeeUpdate
*/
export interface EmployeeUpdate {
    globalRoles: UserRole[]
}

/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyContactUpdate
*/
export interface FamilyContactUpdate {
    childId: UUID
    contactPersonId: UUID
    priority: number | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.GetOrCreatePersonBySsnRequest
*/
export interface GetOrCreatePersonBySsnRequest {
    readonly: boolean
    ssn: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.ParentshipRequest
*/
export interface ParentshipRequest {
    childId: UUID
    endDate: LocalDate
    headOfChildId: UUID
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.ParentshipUpdateRequest
*/
export interface ParentshipUpdateRequest {
    endDate: LocalDate
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.PartnershipRequest
*/
export interface PartnershipRequest {
    endDate: LocalDate | null
    personIds: UUID[]
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.PartnershipUpdateRequest
*/
export interface PartnershipUpdateRequest {
    endDate: LocalDate | null
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.AddSsnRequest
*/
export interface AddSsnRequest {
    ssn: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.MergeRequest
*/
export interface MergeRequest {
    duplicate: UUID
    master: UUID
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.PersonIdentityResponseJSON
*/
export interface PersonIdentityResponseJSON {
    id: UUID
    socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PinCode
*/
export interface PinCode {
    pin: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.SearchEmployeeRequest
*/
export interface SearchEmployeeRequest {
    page: number | null
    pageSize: number | null
    searchTerm: string | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.SearchPersonBody
*/
export interface SearchPersonBody {
    orderBy: string
    searchTerm: string
    sortDirection: string
}

/**
* Generated from fi.espoo.evaka.pis.service.ContactInfo
*/
export interface ContactInfo {
    backupPhone: string
    email: string
    forceManualFeeDecisions: boolean
    invoiceRecipientName: string | null
    invoicingPostOffice: string | null
    invoicingPostalCode: string | null
    invoicingStreetAddress: string | null
    phone: string
}

/**
* Generated from fi.espoo.evaka.pis.service.FamilyOverview
*/
export interface FamilyOverview {
    children: FamilyOverviewPerson[]
    headOfFamily: FamilyOverviewPerson
    partner: FamilyOverviewPerson | null
    totalIncome: number | null
    totalIncomeEffect: IncomeEffect
}

/**
* Generated from fi.espoo.evaka.pis.service.FamilyOverviewPerson
*/
export interface FamilyOverviewPerson {
    dateOfBirth: LocalDate
    firstName: string
    headOfChild: UUID | null
    incomeEffect: IncomeEffect | null
    incomeId: UUID | null
    incomeTotal: number | null
    lastName: string
    personId: UUID
    postOffice: string
    postalCode: string
    restrictedDetailsEnabled: boolean
    streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.service.Parentship
*/
export interface Parentship {
    child: PersonJSON
    childId: UUID
    conflict: boolean
    endDate: LocalDate
    headOfChild: PersonJSON
    headOfChildId: UUID
    id: UUID
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.service.Partnership
*/
export interface Partnership {
    conflict: boolean
    endDate: LocalDate | null
    id: UUID
    partners: PersonJSON[]
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonAddressDTO
*/
export interface PersonAddressDTO {
    city: string
    origin: Origin
    postalCode: string
    residenceCode: string | null
    streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonAddressDTO.Origin
*/
export type Origin = 'VTJ' | 'MUNICIPAL' | 'EVAKA'

/**
* Generated from fi.espoo.evaka.pis.service.PersonJSON
*/
export interface PersonJSON {
    backupPhone: string
    dateOfBirth: LocalDate
    dateOfDeath: LocalDate | null
    email: string | null
    firstName: string | null
    forceManualFeeDecisions: boolean
    id: UUID
    invoiceRecipientName: string
    invoicingPostOffice: string
    invoicingPostalCode: string
    invoicingStreetAddress: string
    language: string | null
    lastName: string | null
    phone: string | null
    postOffice: string | null
    postalCode: string | null
    residenceCode: string | null
    restrictedDetailsEnabled: boolean
    socialSecurityNumber: string | null
    streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonWithChildrenDTO
*/
export interface PersonWithChildrenDTO {
    addresses: PersonAddressDTO[]
    children: PersonWithChildrenDTO[]
    dateOfBirth: LocalDate
    dateOfDeath: LocalDate | null
    firstName: string
    id: UUID
    lastName: string
    nationalities: Nationality[]
    nativeLanguage: NativeLanguage | null
    residenceCode: string | null
    restrictedDetails: RestrictedDetails
    socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.pis.service.RestrictedDetails
*/
export interface RestrictedDetails {
    enabled: boolean
    endDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.placement.ChildBasics
*/
export interface ChildBasics {
    dateOfBirth: LocalDate
    firstName: string | null
    id: UUID
    lastName: string | null
    socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.placement.DaycareBasics
*/
export interface DaycareBasics {
    area: string
    id: UUID
    name: string
    providerType: ProviderType
}

/**
* Generated from fi.espoo.evaka.placement.DaycareGroupPlacement
*/
export interface DaycareGroupPlacement {
    daycarePlacementId: UUID
    endDate: LocalDate
    groupId: UUID | null
    groupName: string | null
    id: UUID | null
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.placement.DaycarePlacementWithDetails
*/
export interface DaycarePlacementWithDetails {
    child: ChildBasics
    daycare: DaycareBasics
    endDate: LocalDate
    groupPlacements: DaycareGroupPlacement[]
    id: UUID
    isRestrictedFromUser: boolean
    missingServiceNeedDays: number
    serviceNeeds: ServiceNeed[]
    startDate: LocalDate
    type: PlacementType
}

/**
* Generated from fi.espoo.evaka.placement.GroupPlacementRequestBody
*/
export interface GroupPlacementRequestBody {
    endDate: LocalDate
    groupId: UUID
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.placement.GroupTransferRequestBody
*/
export interface GroupTransferRequestBody {
    groupId: UUID
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.placement.PlacementCreateRequestBody
*/
export interface PlacementCreateRequestBody {
    childId: UUID
    endDate: LocalDate
    startDate: LocalDate
    type: PlacementType
    unitId: UUID
}

/**
* Generated from fi.espoo.evaka.placement.PlacementDraftChild
*/
export interface PlacementDraftChild {
    dob: LocalDate
    firstName: string
    id: UUID
    lastName: string
}

/**
* Generated from fi.espoo.evaka.placement.PlacementDraftPlacement
*/
export interface PlacementDraftPlacement {
    childId: UUID
    endDate: LocalDate
    id: UUID
    startDate: LocalDate
    type: PlacementType
    unit: PlacementDraftUnit
}

/**
* Generated from fi.espoo.evaka.placement.PlacementDraftUnit
*/
export interface PlacementDraftUnit {
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.placement.PlacementPlanChild
*/
export interface PlacementPlanChild {
    dateOfBirth: LocalDate
    firstName: string
    id: UUID
    lastName: string
}

/**
* Generated from fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
*/
export type PlacementPlanConfirmationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

/**
* Generated from fi.espoo.evaka.placement.PlacementPlanDetails
*/
export interface PlacementPlanDetails {
    applicationId: UUID
    child: PlacementPlanChild
    id: UUID
    period: FiniteDateRange
    preschoolDaycarePeriod: FiniteDateRange | null
    type: PlacementType
    unitConfirmationStatus: PlacementPlanConfirmationStatus
    unitId: UUID
    unitRejectOtherReason: string | null
    unitRejectReason: PlacementPlanRejectReason | null
}

/**
* Generated from fi.espoo.evaka.placement.PlacementPlanDraft
*/
export interface PlacementPlanDraft {
    child: PlacementDraftChild
    guardianHasRestrictedDetails: boolean
    period: FiniteDateRange
    placements: PlacementDraftPlacement[]
    preferredUnits: PlacementDraftUnit[]
    preschoolDaycarePeriod: FiniteDateRange | null
    type: PlacementType
}

/**
* Generated from fi.espoo.evaka.placement.PlacementPlanRejectReason
*/
export type PlacementPlanRejectReason = 'OTHER' | 'REASON_1' | 'REASON_2' | 'REASON_3'

/**
* Generated from fi.espoo.evaka.placement.PlacementType
*/
export type PlacementType = 'CLUB' | 'DAYCARE' | 'DAYCARE_PART_TIME' | 'DAYCARE_FIVE_YEAR_OLDS' | 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE' | 'PREPARATORY' | 'PREPARATORY_DAYCARE' | 'TEMPORARY_DAYCARE' | 'TEMPORARY_DAYCARE_PART_DAY' | 'SCHOOL_SHIFT_CARE'

/**
* Generated from fi.espoo.evaka.placement.PlacementUpdateRequestBody
*/
export interface PlacementUpdateRequestBody {
    endDate: LocalDate
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.reports.ApplicationsReportRow
*/
export interface ApplicationsReportRow {
    careAreaName: string
    club: number
    over3Years: number
    preschool: number
    total: number
    under3Years: number
    unitId: UUID
    unitName: string
    unitProviderType: string
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportRow
*/
export interface AssistanceNeedsAndActionsReportRow {
    assistanceServiceChild: number
    assistanceServiceUnit: number
    autism: number
    careAreaName: string
    developmentMonitoring: number
    developmentMonitoringPending: number
    developmentalDisability1: number
    developmentalDisability2: number
    disability: number
    focusChallenge: number
    groupId: UUID
    groupName: string
    linguisticChallenge: number
    longTermCondition: number
    multiDisability: number
    noAssistanceActions: number
    noAssistanceNeeds: number
    otherAssistanceAction: number
    otherAssistanceNeed: number
    periodicalVeoSupport: number
    pervasiveVeoSupport: number
    ratioDecrease: number
    regulationSkillChallenge: number
    resourcePerson: number
    smallerGroup: number
    specialGroup: number
    unitId: UUID
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ChildAgeLanguageReportRow
*/
export interface ChildAgeLanguageReportRow {
    careAreaName: string
    fi_0y: number
    fi_1y: number
    fi_2y: number
    fi_3y: number
    fi_4y: number
    fi_5y: number
    fi_6y: number
    fi_7y: number
    other_0y: number
    other_1y: number
    other_2y: number
    other_3y: number
    other_4y: number
    other_5y: number
    other_6y: number
    other_7y: number
    sv_0y: number
    sv_1y: number
    sv_2y: number
    sv_3y: number
    sv_4y: number
    sv_5y: number
    sv_6y: number
    sv_7y: number
    unitId: UUID
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ChildrenInDifferentAddressReportRow
*/
export interface ChildrenInDifferentAddressReportRow {
    addressChild: string
    addressParent: string
    careAreaName: string
    childId: UUID
    firstNameChild: string | null
    firstNameParent: string | null
    lastNameChild: string | null
    lastNameParent: string | null
    parentId: UUID
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.Contact
*/
export interface Contact {
    email: string
    firstName: string
    lastName: string
    phone: string
}

/**
* Generated from fi.espoo.evaka.reports.DecisionsReportRow
*/
export interface DecisionsReportRow {
    careAreaName: string
    club: number
    daycareOver3: number
    daycareUnder3: number
    preference1: number
    preference2: number
    preference3: number
    preferenceNone: number
    preparatory: number
    preparatoryDaycare: number
    preschool: number
    preschoolDaycare: number
    providerType: string
    total: number
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.DuplicatePeopleReportRow
*/
export interface DuplicatePeopleReportRow {
    dateOfBirth: LocalDate
    duplicateNumber: number
    firstName: string | null
    groupIndex: number
    id: UUID
    lastName: string | null
    referenceCounts: ReferenceCount[]
    socialSecurityNumber: string | null
    streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.reports.EndedPlacementsReportRow
*/
export interface EndedPlacementsReportRow {
    childId: UUID
    firstName: string | null
    lastName: string | null
    nextPlacementStart: LocalDate | null
    placementEnd: LocalDate
    ssn: string | null
}

/**
* Generated from fi.espoo.evaka.reports.FamilyConflictReportRow
*/
export interface FamilyConflictReportRow {
    careAreaName: string
    childConflictCount: number
    firstName: string | null
    id: UUID
    lastName: string | null
    partnerConflictCount: number
    socialSecurityNumber: string | null
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.FamilyContactReportRow
*/
export interface FamilyContactReportRow {
    firstName: string
    group: string | null
    guardian1: Contact | null
    guardian2: Contact | null
    headOfChild: Contact | null
    id: UUID
    lastName: string
    postOffice: string
    postalCode: string
    ssn: string | null
    streetAddress: string
}

/**
* Generated from fi.espoo.evaka.reports.InvoiceReport
*/
export interface InvoiceReport {
    reportRows: InvoiceReportRow[]
    totalAmountOfInvoices: number
    totalAmountWithZeroPrice: number
    totalAmountWithoutAddress: number
    totalAmountWithoutSSN: number
    totalSumCents: number
}

/**
* Generated from fi.espoo.evaka.reports.InvoiceReportRow
*/
export interface InvoiceReportRow {
    amountOfInvoices: number
    amountWithZeroPrice: number
    amountWithoutAddress: number
    amountWithoutSSN: number
    areaCode: number
    totalSumCents: number
}

/**
* Generated from fi.espoo.evaka.reports.MissingHeadOfFamilyReportRow
*/
export interface MissingHeadOfFamilyReportRow {
    careAreaName: string
    childId: UUID
    daysWithoutHead: number
    firstName: string | null
    lastName: string | null
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.MissingServiceNeedReportRow
*/
export interface MissingServiceNeedReportRow {
    careAreaName: string
    childId: UUID
    daysWithoutServiceNeed: number
    firstName: string | null
    lastName: string | null
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.PartnersInDifferentAddressReportRow
*/
export interface PartnersInDifferentAddressReportRow {
    address1: string
    address2: string
    careAreaName: string
    firstName1: string | null
    firstName2: string | null
    lastName1: string | null
    lastName2: string | null
    personId1: UUID
    personId2: UUID
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.PlacementSketchingReportRow
*/
export interface PlacementSketchingReportRow {
    applicationId: UUID | null
    areaName: string
    assistanceNeeded: boolean | null
    childDob: string | null
    childFirstName: string | null
    childId: UUID
    childLastName: string | null
    childStreetAddr: string | null
    connectedDaycare: boolean | null
    currentUnitId: UUID | null
    currentUnitName: string | null
    guardianEmail: string | null
    guardianPhoneNumber: string | null
    otherPreferredUnits: string[]
    preferredStartDate: LocalDate
    preparatoryEducation: boolean | null
    requestedUnitId: UUID
    requestedUnitName: string
    sentDate: LocalDate
    siblingBasis: boolean | null
}

/**
* Generated from fi.espoo.evaka.reports.PresenceReportRow
*/
export interface PresenceReportRow {
    date: LocalDate
    daycareGroupName: string | null
    daycareId: UUID | null
    present: boolean | null
    socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.reports.RawReportRow
*/
export interface RawReportRow {
    absenceFree: AbsenceType | null
    absencePaid: AbsenceType | null
    age: number
    backupGroupId: UUID | null
    backupUnitId: UUID | null
    capacity: number
    capacityFactor: number
    careArea: string
    caretakersPlanned: number | null
    caretakersRealized: number | null
    childId: UUID
    costCenter: string | null
    dateOfBirth: LocalDate
    day: LocalDate
    daycareGroupId: UUID | null
    groupName: string | null
    hasAssistanceNeed: boolean
    hasServiceNeed: boolean
    hoursPerWeek: number | null
    language: string | null
    partDay: boolean | null
    partWeek: boolean | null
    placementType: PlacementType
    postOffice: string
    shiftCare: boolean | null
    unitId: UUID
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ReferenceCount
*/
export interface ReferenceCount {
    column: string
    count: number
    table: string
}

/**
* Generated from fi.espoo.evaka.reports.ServiceNeedReportRow
*/
export interface ServiceNeedReportRow {
    age: number
    careAreaName: string
    fullDay: number
    fullWeek: number
    missingServiceNeed: number
    partDay: number
    partWeek: number
    shiftCare: number
    total: number
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.ServiceVoucherReport
*/
export interface ServiceVoucherReport {
    locked: LocalDate | null
    rows: ServiceVoucherValueUnitAggregate[]
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.ServiceVoucherUnitReport
*/
export interface ServiceVoucherUnitReport {
    locked: LocalDate | null
    rows: ServiceVoucherValueRow[]
    voucherTotal: number
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueRow
*/
export interface ServiceVoucherValueRow {
    areaId: UUID
    areaName: string
    childDateOfBirth: LocalDate
    childFirstName: string
    childGroupName: string | null
    childId: UUID
    childLastName: string
    isNew: boolean
    numberOfDays: number
    realizedAmount: number
    realizedPeriod: FiniteDateRange
    serviceNeedDescription: string
    serviceVoucherCoPayment: number
    serviceVoucherDecisionId: UUID
    serviceVoucherValue: number
    type: VoucherReportRowType
    unitId: UUID
    unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueUnitAggregate
*/
export interface ServiceVoucherValueUnitAggregate {
    childCount: number
    monthlyPaymentSum: number
    unit: UnitData
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueUnitAggregate.UnitData
*/
export interface UnitData {
    areaId: UUID
    areaName: string
    id: UUID
    name: string
}

/**
* Generated from fi.espoo.evaka.reports.StartingPlacementsRow
*/
export interface StartingPlacementsRow {
    careAreaName: string
    childId: UUID
    dateOfBirth: LocalDate
    firstName: string
    lastName: string
    placementStart: LocalDate
    ssn: string | null
}

/**
* Generated from fi.espoo.evaka.reports.UnitType
*/
export type UnitType = 'DAYCARE' | 'FAMILY' | 'GROUP_FAMILY' | 'CLUB'

/**
* Generated from fi.espoo.evaka.reports.VardaErrorReportRow
*/
export interface VardaErrorReportRow {
    childId: UUID
    errors: string[]
    serviceNeedEndDate: string
    serviceNeedId: UUID
    serviceNeedOptionName: string
    serviceNeedStartDate: string
    updated: Date
}

/**
* Generated from fi.espoo.evaka.reports.VoucherReportRowType
*/
export type VoucherReportRowType = 'ORIGINAL' | 'REFUND' | 'CORRECTION'

/**
* Generated from fi.espoo.evaka.reservations.AbsenceRequest
*/
export interface AbsenceRequest {
    absenceType: AbsenceType
    childIds: UUID[]
    dateRange: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.reservations.ChildDailyData
*/
export interface ChildDailyData {
    absence: AbsenceType | null
    childId: UUID
    reservation: Reservation | null
}

/**
* Generated from fi.espoo.evaka.reservations.DailyReservationData
*/
export interface DailyReservationData {
    children: ChildDailyData[]
    date: LocalDate
    isHoliday: boolean
}

/**
* Generated from fi.espoo.evaka.reservations.DailyReservationRequest
*/
export interface DailyReservationRequest {
    childId: UUID
    date: LocalDate
    endTime: string
    startTime: string
}

/**
* Generated from fi.espoo.evaka.reservations.Reservation
*/
export interface Reservation {
    endTime: Date
    startTime: Date
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationChild
*/
export interface ReservationChild {
    firstName: string
    id: UUID
    preferredName: string | null
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
    children: ReservationChild[]
    dailyData: DailyReservationData[]
    reservableDays: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeed
*/
export interface ServiceNeed {
    confirmed: ServiceNeedConfirmation | null
    endDate: LocalDate
    id: UUID
    option: ServiceNeedOptionSummary
    placementId: UUID
    shiftCare: boolean
    startDate: LocalDate
    updated: Date
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedConfirmation
*/
export interface ServiceNeedConfirmation {
    at: Date | null
    employeeId: UUID
    firstName: string
    lastName: string
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.ServiceNeedCreateRequest
*/
export interface ServiceNeedCreateRequest {
    endDate: LocalDate
    optionId: UUID
    placementId: UUID
    shiftCare: boolean
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedController.ServiceNeedUpdateRequest
*/
export interface ServiceNeedUpdateRequest {
    endDate: LocalDate
    optionId: UUID
    shiftCare: boolean
    startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOption
*/
export interface ServiceNeedOption {
    daycareHoursPerWeek: number
    defaultOption: boolean
    feeCoefficient: number
    feeDescriptionFi: string
    feeDescriptionSv: string
    id: UUID
    name: string
    occupancyCoefficient: number
    partDay: boolean
    partWeek: boolean
    updated: Date
    validPlacementType: PlacementType
    voucherValueCoefficient: number
    voucherValueDescriptionFi: string
    voucherValueDescriptionSv: string
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOptionPublicInfo
*/
export interface ServiceNeedOptionPublicInfo {
    id: UUID
    name: string
    validPlacementType: PlacementType
}

/**
* Generated from fi.espoo.evaka.serviceneed.ServiceNeedOptionSummary
*/
export interface ServiceNeedOptionSummary {
    id: UUID
    name: string
    updated: Date
}

/**
* Generated from fi.espoo.evaka.shared.auth.DaycareAclRow
*/
export interface DaycareAclRow {
    employee: DaycareAclRowEmployee
    groupIds: UUID[]
    role: UserRole
}

/**
* Generated from fi.espoo.evaka.shared.auth.DaycareAclRowEmployee
*/
export interface DaycareAclRowEmployee {
    email: string | null
    firstName: string
    id: UUID
    lastName: string
}

/**
* Generated from fi.espoo.evaka.shared.auth.UserRole
*/
export type UserRole = 'END_USER' | 'CITIZEN_WEAK' | 'ADMIN' | 'DIRECTOR' | 'FINANCE_ADMIN' | 'SERVICE_WORKER' | 'UNIT_SUPERVISOR' | 'STAFF' | 'SPECIAL_EDUCATION_TEACHER' | 'MOBILE' | 'GROUP_STAFF'

/**
* Generated from fi.espoo.evaka.shared.controllers.ScheduledJobTriggerController.TriggerBody
*/
export interface TriggerBody {
    type: ScheduledJob
}

/**
* Generated from fi.espoo.evaka.shared.domain.Coordinate
*/
export interface Coordinate {
    lat: number
    lon: number
}

/**
* Generated from fi.espoo.evaka.shared.job.ScheduledJob
*/
export type ScheduledJob = 'CancelOutdatedTransferApplications' | 'DvvUpdate' | 'EndOfDayAttendanceUpkeep' | 'EndOutdatedVoucherValueDecisions' | 'FreezeVoucherValueReports' | 'KoskiUpdate' | 'RemoveOldAsyncJobs' | 'RemoveOldDaycareDailyNotes' | 'RemoveOldDraftApplications' | 'SendPendingDecisionReminderEmails' | 'VardaUpdate' | 'InactivePeopleCleanup' | 'InactiveEmployeesRoleReset'

/**
* Generated from fi.espoo.evaka.shared.security.CitizenFeatures
*/
export interface CitizenFeatures {
    messages: boolean
    reservations: boolean
}

/**
* Generated from fi.espoo.evaka.shared.security.EmployeeFeatures
*/
export interface EmployeeFeatures {
    applications: boolean
    employees: boolean
    finance: boolean
    financeBasics: boolean
    messages: boolean
    personSearch: boolean
    reports: boolean
    units: boolean
    vasuTemplates: boolean
}

/**
* Generated from fi.espoo.evaka.shared.security.PilotFeature
*/
export type PilotFeature = 'MESSAGING' | 'MOBILE' | 'RESERVATIONS'

/**
* Generated from fi.espoo.evaka.varda.VardaChildRequest
*/
export interface VardaChildRequest {
    henkilo: string | null
    henkilo_oid: string | null
    id: UUID
    lahdejarjestelma: string
    oma_organisaatio_oid: string | null
    paos_organisaatio_oid: string | null
    vakatoimija_oid: string | null
}

/**
* Generated from fi.espoo.evaka.varda.VardaDecision
*/
export interface VardaDecision {
    applicationDate: LocalDate
    childUrl: string
    daily: boolean
    endDate: LocalDate
    fullDay: boolean
    hoursPerWeek: number
    providerTypeCode: string
    shiftCare: boolean
    sourceSystem: string
    startDate: LocalDate
    temporary: boolean
    urgent: boolean
}

/**
* Generated from fi.espoo.evaka.varda.VardaFeeData
*/
export interface VardaFeeData {
    alkamis_pvm: LocalDate
    asiakasmaksu: number
    huoltajat: VardaGuardian[]
    lahdejarjestelma: string
    lapsi: string
    maksun_peruste_koodi: string
    paattymis_pvm: LocalDate | null
    palveluseteli_arvo: number
    perheen_koko: number
}

/**
* Generated from fi.espoo.evaka.varda.VardaGuardian
*/
export interface VardaGuardian {
    etunimet: string
    henkilo_oid: string | null
    henkilotunnus: string | null
    sukunimi: string
}

/**
* Generated from fi.espoo.evaka.varda.VardaPersonRequest
*/
export interface VardaPersonRequest {
    firstName: string
    id: UUID
    lastName: string
    nickName: string
    personOid: string | null
    ssn: string | null
}

/**
* Generated from fi.espoo.evaka.varda.VardaPlacement
*/
export interface VardaPlacement {
    decisionUrl: string
    endDate: LocalDate | null
    sourceSystem: string
    startDate: LocalDate
    unitOid: string
}

/**
* Generated from fi.espoo.evaka.varda.VardaUnitRequest
*/
export interface VardaUnitRequest {
    alkamis_pvm: string | null
    asiointikieli_koodi: string[]
    id: number | null
    jarjestamismuoto_koodi: string[]
    kasvatusopillinen_jarjestelma_koodi: string | null
    kayntiosoite: string | null
    kayntiosoite_postinumero: string | null
    kayntiosoite_postitoimipaikka: string | null
    kielipainotus_kytkin: boolean
    kunta_koodi: string | null
    lahdejarjestelma: string | null
    nimi: string | null
    organisaatio_oid: string | null
    paattymis_pvm: string | null
    postinumero: string | null
    postiosoite: string | null
    postitoimipaikka: string | null
    puhelinnumero: string | null
    sahkopostiosoite: string | null
    toiminnallinenpainotus_kytkin: boolean | null
    toimintamuoto_koodi: string | null
    vakajarjestaja: string | null
    varhaiskasvatuspaikat: number
}

/**
* Generated from fi.espoo.evaka.varda.VardaUpdateOrganizer
*/
export interface VardaUpdateOrganizer {
    email: string | null
    phone: string | null
    vardaOrganizerId: number
}

/**
* Generated from fi.espoo.evaka.varda.integration.VardaClient.DecisionPeriod
*/
export interface DecisionPeriod {
    alkamis_pvm: LocalDate
    id: number
    paattymis_pvm: LocalDate
}

/**
* Generated from fi.espoo.evaka.vtjclient.controllers.VtjController.Child
*/
export interface Child {
    firstName: string
    id: UUID
    lastName: string
    socialSecurityNumber: string
}

/**
* Generated from fi.espoo.evaka.vtjclient.controllers.VtjController.CitizenUserDetails
*/
export interface CitizenUserDetails {
    accessibleFeatures: CitizenFeatures
    children: Child[]
    firstName: string
    id: UUID
    lastName: string
    socialSecurityNumber: string
}

/**
* Generated from fi.espoo.evaka.vtjclient.dto.Nationality
*/
export interface Nationality {
    countryCode: string
    countryName: string
}

/**
* Generated from fi.espoo.evaka.vtjclient.dto.NativeLanguage
*/
export interface NativeLanguage {
    code: string
    languageName: string
}