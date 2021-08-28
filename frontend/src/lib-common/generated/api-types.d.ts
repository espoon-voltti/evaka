// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from "../local-date";
import {DailyServiceTimes} from "../api-types/child/common";

export interface Address {
    postOffice: string
    postalCode: string
    street: string
}

export interface Reservation {
    childId: string
    endTime: Date
    startTime: Date
}

export type VoucherReportRowType = 'ORIGINAL' | 'REFUND' | 'CORRECTION'

export interface UnitData {
    areaId: string
    areaName: string
    id: string
    name: string
}

export interface ServiceNeedOptionSummary {
    id: string
    name: string
    updated: Date
}

export interface ServiceNeedConfirmation {
    at: Date
    employeeId: string
    firstName: string
    lastName: string
}

export type Origin = 'VTJ' | 'MUNICIPAL' | 'EVAKA'

export interface MessageReceiverPerson {
    accountId: string
    receiverFirstName: string
    receiverLastName: string
}

export interface WithDateOfBirth {
    dateOfBirth: LocalDate
    id: string
}

export interface DaycareAclRowEmployee {
    email: string | null
    firstName: string
    id: string
    lastName: string
}

export interface BackupCareChild {
    birthDate: LocalDate
    firstName: string
    id: string
    lastName: string
}

export interface BackupCareUnit {
    id: string
    name: string
}

export interface BackupCareGroup {
    id: string
    name: string
}

export interface ContactInfo {
    backupPhone: string
    email: string
    firstName: string
    id: string
    lastName: string
    phone: string
    priority: number | null
}

export interface Staff {
    firstName: string
    groups: string[]
    id: string
    lastName: string
    pinSet: boolean
}

export interface GroupInfo {
    dailyNote: DaycareDailyNote | null
    id: string
    name: string
}

export type AttendanceStatus = 'COMING' | 'PRESENT' | 'DEPARTED' | 'ABSENT'

export interface AttendanceReservation {
    endTime: string
    startTime: string
}

export interface ChildAttendance {
    arrived: Date
    childId: string
    departed: Date | null
    id: string
    unitId: string
}

export interface ChildAbsence {
    careType: AbsenceCareType
    childId: string
    id: string
}

export type OtherGuardianAgreementStatus = 'AGREED' | 'NOT_AGREED' | 'RIGHT_TO_GET_NOTIFIED'

export interface SiblingBasis {
    siblingName: string
    siblingSsn: string
}

export interface ServiceNeed {
    endTime: string
    partTime: boolean
    serviceNeedOption: ServiceNeedOption | null
    shiftCare: boolean
    startTime: string
}

export interface FutureAddress {
    movingDate: LocalDate | null
    postOffice: string
    postalCode: string
    street: string
}

export interface Guardian {
    address: Address | null
    email: string
    futureAddress: FutureAddress | null
    person: PersonBasics
    phoneNumber: string
}

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

export type AttachmentType = 'URGENCY' | 'EXTENDED_CARE'

export interface Child {
    firstName: string
    id: string
    lastName: string
    socialSecurityNumber: string
}

export interface CitizenFeatures {
    messages: boolean
    reservations: boolean
}

export interface VardaGuardian {
    etunimet: string
    henkilo_oid: string | null
    henkilotunnus: string | null
    sukunimi: string
}

export type ScheduledJob = 'CancelOutdatedTransferApplications' | 'DvvUpdate' | 'EndOfDayAttendanceUpkeep' | 'EndOutdatedVoucherValueDecisions' | 'FreezeVoucherValueReports' | 'KoskiUpdate' | 'RemoveOldAsyncJobs' | 'RemoveOldDaycareDailyNotes' | 'RemoveOldDraftApplications' | 'SendPendingDecisionReminderEmails' | 'VardaUpdate' | 'InactivePeopleCleanup' | 'InactiveEmployeesRoleReset'

export interface DailyReservationRequest {
    date: LocalDate
    endTime: string
    startTime: string
}

export interface DailyReservationData {
    date: LocalDate
    isHoliday: boolean
    reservations: Reservation[]
}

export interface ReservationChild {
    firstName: string
    id: string
    preferredName: string | null
}

export interface ServiceVoucherValueRow {
    areaId: string
    areaName: string
    childDateOfBirth: LocalDate
    childFirstName: string
    childGroupName: string | null
    childId: string
    childLastName: string
    isNew: boolean
    numberOfDays: number
    realizedAmount: number
    realizedPeriod: FiniteDateRange
    serviceNeedDescription: string
    serviceVoucherCoPayment: number
    serviceVoucherDecisionId: string
    serviceVoucherValue: number
    type: VoucherReportRowType
    unitId: string
    unitName: string
}

export interface ServiceVoucherValueUnitAggregate {
    childCount: number
    monthlyPaymentSum: number
    unit: UnitData
}

export interface InvoiceReportRow {
    amountOfInvoices: number
    amountWithZeroPrice: number
    amountWithoutAddress: number
    amountWithoutSSN: number
    areaCode: number
    totalSumCents: number
}

export interface Contact {
    email: string
    firstName: string
    lastName: string
    phone: string
}

export interface ReferenceCount {
    column: string
    count: number
    table: string
}

export type UnitType = 'DAYCARE' | 'FAMILY' | 'GROUP_FAMILY' | 'CLUB'

export interface ServiceNeed {
    confirmed: ServiceNeedConfirmation
    endDate: LocalDate
    id: string
    option: ServiceNeedOptionSummary
    placementId: string
    shiftCare: boolean
    startDate: LocalDate
    updated: Date
}

export interface DaycareGroupPlacement {
    daycarePlacementId: string
    endDate: LocalDate
    groupId: string | null
    groupName: string | null
    id: string | null
    startDate: LocalDate
}

export interface DaycareBasics {
    area: string
    id: string
    name: string
    providerType: ProviderType
}

export interface ChildBasics {
    dateOfBirth: LocalDate
    firstName: string | null
    id: string
    lastName: string | null
    socialSecurityNumber: string | null
}

export interface PlacementPlanChild {
    dateOfBirth: LocalDate
    firstName: string
    id: string
    lastName: string
}

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

export interface RestrictedDetails {
    enabled: boolean
    endDate: LocalDate | null
}

export interface NativeLanguage {
    code: string
    languageName: string
}

export interface Nationality {
    countryCode: string
    countryName: string
}

export interface PersonAddressDTO {
    city: string
    origin: Origin
    postalCode: string
    residenceCode: string | null
    streetAddress: string
}

export type FamilyContactRole = 'LOCAL_GUARDIAN' | 'LOCAL_ADULT' | 'LOCAL_SIBLING' | 'REMOTE_GUARDIAN'

export type IncomeEffect = 'MAX_FEE_ACCEPTED' | 'INCOMPLETE' | 'INCOME' | 'NOT_AVAILABLE'

export interface FamilyOverviewPerson {
    dateOfBirth: LocalDate
    firstName: string
    headOfChild: string | null
    incomeEffect: IncomeEffect | null
    incomeId: string | null
    incomeTotal: number | null
    lastName: string
    personId: string
    postOffice: string
    postalCode: string
    restrictedDetailsEnabled: boolean
    streetAddress: string
}

export interface DaycareRole {
    daycareId: string
    daycareName: string
    role: UserRole
}

export type UserRole = 'END_USER' | 'CITIZEN_WEAK' | 'ADMIN' | 'DIRECTOR' | 'FINANCE_ADMIN' | 'SERVICE_WORKER' | 'UNIT_SUPERVISOR' | 'STAFF' | 'SPECIAL_EDUCATION_TEACHER' | 'MOBILE' | 'GROUP_STAFF'

export interface ExternalId {
    namespace: string
    value: string
}

export type PairingStatus = 'WAITING_CHALLENGE' | 'WAITING_RESPONSE' | 'READY' | 'PAIRED'

export interface OccupancyPeriod {
    caretakers: number | null
    headcount: number
    percentage: number | null
    period: FiniteDateRange
    sum: number
}

export interface MessageReceiver {
    childDateOfBirth: LocalDate
    childFirstName: string
    childId: string
    childLastName: string
    receiverPersons: MessageReceiverPerson[]
}

export interface Message {
    content: string
    id: string
    readAt: Date | null
    recipients: MessageAccount[]
    senderId: string
    senderName: string
    sentAt: Date
}

export type AccountType = 'PERSONAL' | 'GROUP'

export interface Group {
    id: string
    name: string
    unitId: string
    unitName: string
}

export type MessageType = 'MESSAGE' | 'BULLETIN'

export type DaycareDailyNoteReminder = 'DIAPERS' | 'CLOTHES' | 'LAUNDRY'

export type DaycareDailyNoteLevelInfo = 'GOOD' | 'MEDIUM' | 'NONE'

export type VoucherValueDecisionStatus = 'DRAFT' | 'WAITING_FOR_SENDING' | 'WAITING_FOR_MANUAL_SENDING' | 'SENT' | 'ANNULLED'

export interface InvoiceRowSummary {
    amount: number
    child: Basic
    id: string
    unitPrice: number
}

export type Product = 'DAYCARE' | 'DAYCARE_DISCOUNT' | 'DAYCARE_INCREASE' | 'PRESCHOOL_WITH_DAYCARE' | 'PRESCHOOL_WITH_DAYCARE_DISCOUNT' | 'PRESCHOOL_WITH_DAYCARE_INCREASE' | 'TEMPORARY_CARE' | 'SCHOOL_SHIFT_CARE' | 'SICK_LEAVE_100' | 'SICK_LEAVE_50' | 'ABSENCE' | 'FREE_OF_CHARGE'

export interface InvoiceRowDetailed {
    amount: number
    child: Detailed
    costCenter: string
    description: string
    id: string
    periodEnd: LocalDate
    periodStart: LocalDate
    product: Product
    subCostCenter: string | null
    unitPrice: number
}

export interface Detailed {
    dateOfBirth: LocalDate
    dateOfDeath: LocalDate | null
    email: string | null
    firstName: string
    forceManualFeeDecisions: boolean
    id: string
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

export type InvoiceStatus = 'DRAFT' | 'WAITING_FOR_SENDING' | 'SENT' | 'CANCELED'

export interface InvoiceRow {
    amount: number
    child: WithDateOfBirth
    costCenter: string
    description: string
    id: string | null
    periodEnd: LocalDate
    periodStart: LocalDate
    product: Product
    subCostCenter: string | null
    unitPrice: number
}

export interface JustId {
    id: string
}

export type FeeDecisionType = 'NORMAL' | 'RELIEF_REJECTED' | 'RELIEF_PARTLY_ACCEPTED' | 'RELIEF_ACCEPTED'

export type FeeDecisionStatus = 'DRAFT' | 'WAITING_FOR_SENDING' | 'WAITING_FOR_MANUAL_SENDING' | 'SENT' | 'ANNULLED'

export interface Basic {
    dateOfBirth: LocalDate
    firstName: string
    id: string
    lastName: string
    ssn: string | null
}

export type Type = 'DISCOUNT' | 'INCREASE' | 'RELIEF'

export interface DaycareAclRow {
    employee: DaycareAclRowEmployee
    groupIds: string[]
    role: UserRole
}

export interface GroupStaffAttendance {
    count: number
    countOther: number
    date: LocalDate
    groupId: string
    updated: Date
}

export interface FinanceDecisionHandler {
    LastName: string
    firstName: string
    id: string
}

export type PilotFeature = 'MESSAGING' | 'MOBILE' | 'RESERVATIONS'

export interface DaycareCareArea {
    id: string
    name: string
    shortName: string
}

export interface CaretakerAmount {
    amount: number
    endDate: LocalDate | null
    groupId: string
    id: string
    startDate: LocalDate
}

export interface VisitingAddress {
    postOffice: string
    postalCode: string
    streetAddress: string
}

export interface UnitManager {
    email: string | null
    name: string | null
    phone: string | null
}

export type CareType = 'CLUB' | 'FAMILY' | 'CENTRE' | 'GROUP_FAMILY' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION'

export type ProviderType = 'MUNICIPAL' | 'PURCHASED' | 'PRIVATE' | 'MUNICIPAL_SCHOOL' | 'PRIVATE_SERVICE_VOUCHER'

export interface MailingAddress {
    poBox: string | null
    postOffice: string | null
    postalCode: string | null
    streetAddress: string | null
}

export interface Coordinate {
    lat: number
    lon: number
}

export type Language = 'fi' | 'sv' | 'en'

export interface DaycareDecisionCustomization {
    daycareName: string
    handler: string
    handlerAddress: string
    preschoolName: string
}

export interface DateRange {
    end: LocalDate | null
    start: LocalDate
}

export interface UnitBackupCare {
    child: BackupCareChild
    group: BackupCareGroup | null
    id: string
    missingServiceNeedDays: number
    period: FiniteDateRange
    serviceNeeds: ServiceNeed[]
}

export interface ChildBackupCare {
    group: BackupCareGroup | null
    id: string
    period: FiniteDateRange
    unit: BackupCareUnit
}

export type AbsenceType = 'OTHER_ABSENCE' | 'SICKLEAVE' | 'UNKNOWN_ABSENCE' | 'PLANNED_ABSENCE' | 'TEMPORARY_RELOCATION' | 'TEMPORARY_VISITOR' | 'PARENTLEAVE' | 'FORCE_MAJEURE'

export type ChildResultStatus = 'SUCCESS' | 'WRONG_PIN' | 'PIN_LOCKED' | 'NOT_FOUND'

export interface ChildSensitiveInformation {
    allergies: string
    backupPickups: ContactInfo[]
    childAddress: string
    contacts: ContactInfo[]
    diet: string
    firstName: string
    id: string
    lastName: string
    medication: string
    placementTypes: PlacementType[]
    preferredName: string
    ssn: string
}

export type AbsenceCareType = 'SCHOOL_SHIFT_CARE' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE' | 'DAYCARE_5YO_FREE' | 'DAYCARE' | 'CLUB'

export interface UnitInfo {
    groups: GroupInfo[]
    id: string
    name: string
    staff: Staff[]
}

export interface Child {
    absences: ChildAbsence[]
    attendance: ChildAttendance | null
    backup: boolean
    dailyNote: DaycareDailyNote | null
    dailyServiceTimes: DailyServiceTimes | null
    firstName: string
    groupId: string
    id: string
    imageUrl: string | null
    lastName: string
    placementType: PlacementType
    preferredName: string | null
    reservation: AttendanceReservation | null
    status: AttendanceStatus
}

export type AssistanceMeasure = 'SPECIAL_ASSISTANCE_DECISION' | 'INTENSIFIED_ASSISTANCE' | 'EXTENDED_COMPULSORY_EDUCATION' | 'CHILD_SERVICE' | 'CHILD_ACCULTURATION_SUPPORT' | 'TRANSPORT_BENEFIT'

export type PlacementPlanConfirmationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export type PlacementPlanRejectReason = 'OTHER' | 'REASON_1' | 'REASON_2' | 'REASON_3'

export interface PlacementDraftUnit {
    id: string
    name: string
}

export interface PlacementDraftPlacement {
    childId: string
    endDate: LocalDate
    id: string
    startDate: LocalDate
    type: PlacementType
    unit: PlacementDraftUnit
}

export interface PlacementDraftChild {
    dob: LocalDate
    firstName: string
    id: string
    lastName: string
}

export interface GuardianInfo {
    firstName: string
    id: string | null
    isVtjGuardian: boolean
    lastName: string
    ssn: string | null
}

export interface DecisionDraft {
    endDate: LocalDate
    id: string
    planned: boolean
    startDate: LocalDate
    type: DecisionType
    unitId: string
}

export interface ChildInfo {
    firstName: string
    lastName: string
    ssn: string | null
}

export interface ServiceNeedOption {
    id: string
    name: string
}

export type PlacementType = 'CLUB' | 'DAYCARE' | 'DAYCARE_PART_TIME' | 'DAYCARE_FIVE_YEAR_OLDS' | 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE' | 'PREPARATORY' | 'PREPARATORY_DAYCARE' | 'TEMPORARY_DAYCARE' | 'TEMPORARY_DAYCARE_PART_DAY' | 'SCHOOL_SHIFT_CARE'

export interface PlacementProposalStatus {
    unitConfirmationStatus: PlacementPlanConfirmationStatus
    unitRejectOtherReason: string | null
    unitRejectReason: PlacementPlanRejectReason | null
}

export interface PreferredUnit {
    id: string
    name: string
}

export interface FiniteDateRange {
    end: LocalDate
    start: LocalDate
}

export interface SecondGuardian {
    agreementStatus: OtherGuardianAgreementStatus | null
    email: string
    phoneNumber: string
}

export interface Preferences {
    preferredStartDate: LocalDate | null
    preferredUnits: PreferredUnit[]
    preparatory: boolean
    serviceNeed: ServiceNeed | null
    siblingBasis: SiblingBasis | null
    urgent: boolean
}

export interface PersonBasics {
    firstName: string
    lastName: string
    socialSecurityNumber: string | null
}

export interface GuardianUpdate {
    email: string
    futureAddress: FutureAddress | null
    phoneNumber: string
}

export interface ClubDetails {
    wasOnClubCare: boolean
    wasOnDaycare: boolean
}

export interface ChildDetailsUpdate {
    allergies: string
    assistanceDescription: string
    assistanceNeeded: boolean
    diet: string
    futureAddress: FutureAddress | null
}

export interface CitizenApplicationSummary {
    allPreferredUnitNames: string[]
    applicationId: string
    applicationStatus: ApplicationStatus
    childId: string
    childName: string | null
    createdDate: Date
    modifiedDate: Date
    preferredUnitName: string | null
    sentDate: LocalDate | null
    startDate: LocalDate | null
    transferApplication: boolean
    type: string
}

export interface DecisionSummary {
    decisionId: string
    resolved: LocalDate | null
    sentDate: LocalDate
    status: DecisionStatus
    type: DecisionType
}

export type DecisionType = 'CLUB' | 'DAYCARE' | 'DAYCARE_PART_TIME' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE' | 'PREPARATORY_EDUCATION'

export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export type ApplicationStatus = 'CREATED' | 'SENT' | 'WAITING_PLACEMENT' | 'WAITING_UNIT_CONFIRMATION' | 'WAITING_DECISION' | 'WAITING_MAILING' | 'WAITING_CONFIRMATION' | 'REJECTED' | 'ACTIVE' | 'CANCELLED'

export type ApplicationOrigin = 'ELECTRONIC' | 'PAPER'

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

export interface Attachment {
    contentType: string
    id: string
    name: string
    receivedAt: Date
    type: AttachmentType
    updated: Date
    uploadedByEmployee: string | null
    uploadedByPerson: string | null
}

export type ApplicationType = 'CLUB' | 'DAYCARE' | 'PRESCHOOL'

export interface CitizenUserDetails {
    accessibleFeatures: CitizenFeatures
    children: Child[]
    firstName: string
    id: string
    lastName: string
    socialSecurityNumber: string
}

export interface VardaUpdateOrganizer {
    email: string | null
    phone: string | null
    vardaOrganizerId: number
}

export interface DecisionPeriod {
    alkamis_pvm: LocalDate
    id: number
    paattymis_pvm: LocalDate
}

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

export interface VardaPlacement {
    decisionUrl: string
    endDate: LocalDate | null
    sourceSystem: string
    startDate: LocalDate
    unitOid: string
}

export interface VardaPersonRequest {
    firstName: string
    id: string
    lastName: string
    nickName: string
    personOid: string | null
    ssn: string | null
}

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

export interface VardaChildRequest {
    henkilo: string | null
    henkilo_oid: string | null
    id: string
    lahdejarjestelma: string
    oma_organisaatio_oid: string | null
    paos_organisaatio_oid: string | null
    vakatoimija_oid: string | null
}

export interface TriggerBody {
    type: ScheduledJob
}

export interface ServiceNeedUpdateRequest {
    endDate: LocalDate
    optionId: string
    shiftCare: boolean
    startDate: LocalDate
}

export interface ServiceNeedCreateRequest {
    endDate: LocalDate
    optionId: string
    placementId: string
    shiftCare: boolean
    startDate: LocalDate
}

export interface ServiceNeedOption {
    daycareHoursPerWeek: number
    defaultOption: boolean
    feeCoefficient: number
    feeDescriptionFi: string
    feeDescriptionSv: string
    id: string
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

export interface ServiceNeedOptionPublicInfo {
    id: string
    name: string
    validPlacementType: PlacementType
}

export interface ReservationRequest {
    children: string[]
    reservations: DailyReservationRequest[]
}

export interface ReservationsResponse {
    children: ReservationChild[]
    dailyData: DailyReservationData[]
    reservableDays: FiniteDateRange
}

export interface StartingPlacementsRow {
    careAreaName: string
    childId: string
    dateOfBirth: LocalDate
    firstName: string
    lastName: string
    placementStart: LocalDate
    ssn: string | null
}

export interface ServiceVoucherUnitReport {
    locked: LocalDate | null
    rows: ServiceVoucherValueRow[]
    voucherTotal: number
}

export interface ServiceVoucherReport {
    locked: LocalDate | null
    rows: ServiceVoucherValueUnitAggregate[]
}

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

export interface RawReportRow {
    absenceFree: AbsenceType | null
    absencePaid: AbsenceType | null
    age: number
    backupGroupId: string | null
    backupUnitId: string | null
    capacity: number
    capacityFactor: number
    careArea: string
    caretakersPlanned: number | null
    caretakersRealized: number | null
    childId: string
    dateOfBirth: LocalDate
    day: LocalDate
    daycareGroupId: string | null
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
    unitId: string
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

export interface PresenceReportRow {
    date: LocalDate
    daycareGroupName: string | null
    daycareId: string | null
    present: boolean | null
    socialSecurityNumber: string | null
}

export interface PlacementSketchingReportRow {
    applicationId: string | null
    areaName: string
    assistanceNeeded: boolean | null
    childDob: string | null
    childFirstName: string | null
    childId: string
    childLastName: string | null
    childStreetAddr: string | null
    connectedDaycare: boolean | null
    currentUnitId: string | null
    currentUnitName: string | null
    guardianEmail: string | null
    guardianPhoneNumber: string | null
    otherPreferredUnits: string[]
    preferredStartDate: LocalDate
    preparatoryEducation: boolean | null
    requestedUnitId: string
    requestedUnitName: string
    sentDate: LocalDate
    siblingBasis: boolean | null
}

export interface PartnersInDifferentAddressReportRow {
    address1: string
    address2: string
    careAreaName: string
    firstName1: string | null
    firstName2: string | null
    lastName1: string | null
    lastName2: string | null
    personId1: string
    personId2: string
    unitId: string
    unitName: string
}

export interface MissingServiceNeedReportRow {
    careAreaName: string
    childId: string
    daysWithoutServiceNeed: number
    firstName: string | null
    lastName: string | null
    unitId: string
    unitName: string
}

export interface MissingHeadOfFamilyReportRow {
    careAreaName: string
    childId: string
    daysWithoutHead: number
    firstName: string | null
    lastName: string | null
    unitId: string
    unitName: string
}

export interface InvoiceReport {
    reportRows: InvoiceReportRow[]
    totalAmountOfInvoices: number
    totalAmountWithZeroPrice: number
    totalAmountWithoutAddress: number
    totalAmountWithoutSSN: number
    totalSumCents: number
}

export interface FamilyContactReportRow {
    firstName: string
    group: string | null
    guardian1: Contact | null
    guardian2: Contact | null
    headOfChild: Contact | null
    id: string
    lastName: string
    postOffice: string
    postalCode: string
    ssn: string | null
    streetAddress: string
}

export interface FamilyConflictReportRow {
    careAreaName: string
    childConflictCount: number
    firstName: string | null
    id: string
    lastName: string | null
    partnerConflictCount: number
    socialSecurityNumber: string | null
    unitId: string
    unitName: string
}

export interface EndedPlacementsReportRow {
    childId: string
    firstName: string | null
    lastName: string | null
    nextPlacementStart: LocalDate | null
    placementEnd: LocalDate
    ssn: string | null
}

export interface DuplicatePeopleReportRow {
    dateOfBirth: LocalDate
    duplicateNumber: number
    firstName: string | null
    groupIndex: number
    id: string
    lastName: string | null
    referenceCounts: ReferenceCount[]
    socialSecurityNumber: string | null
    streetAddress: string | null
}

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
    unitId: string
    unitName: string
}

export interface ChildrenInDifferentAddressReportRow {
    addressChild: string
    addressParent: string
    careAreaName: string
    childId: string
    firstNameChild: string | null
    firstNameParent: string | null
    lastNameChild: string | null
    lastNameParent: string | null
    parentId: string
    unitId: string
    unitName: string
}

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
    unitId: string
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

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
    groupId: string
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
    unitId: string
    unitName: string
    unitProviderType: string
    unitType: UnitType | null
}

export interface ApplicationsReportRow {
    careAreaName: string
    club: number
    over3Years: number
    preschool: number
    total: number
    under3Years: number
    unitId: string
    unitName: string
    unitProviderType: string
}

export interface PlacementUpdateRequestBody {
    endDate: LocalDate
    startDate: LocalDate
}

export interface GroupTransferRequestBody {
    groupId: string
    startDate: LocalDate
}

export interface DaycarePlacementWithDetails {
    child: ChildBasics
    daycare: DaycareBasics
    endDate: LocalDate
    groupPlacements: DaycareGroupPlacement[]
    id: string
    isRestrictedFromUser: boolean
    missingServiceNeedDays: number
    serviceNeeds: ServiceNeed[]
    startDate: LocalDate
    type: PlacementType
}

export interface PlacementPlanDetails {
    applicationId: string
    child: PlacementPlanChild
    id: string
    period: FiniteDateRange
    preschoolDaycarePeriod: FiniteDateRange | null
    type: PlacementType
    unitConfirmationStatus: PlacementPlanConfirmationStatus
    unitId: string
    unitRejectOtherReason: string | null
    unitRejectReason: PlacementPlanRejectReason | null
}

export interface PlacementCreateRequestBody {
    childId: string
    endDate: LocalDate
    startDate: LocalDate
    type: PlacementType
    unitId: string
}

export interface GroupPlacementRequestBody {
    endDate: LocalDate
    groupId: string
    startDate: LocalDate
}

export interface PersonIdentityRequest {
    firstName: string
    lastName: string
    socialSecurityNumber: string
}

export interface EmployeeUserResponse {
    accessibleFeatures: EmployeeFeatures
    allScopedRoles: UserRole[]
    firstName: string
    globalRoles: UserRole[]
    id: string
    lastName: string
}

export interface EmployeeUser {
    allScopedRoles: UserRole[]
    firstName: string
    globalRoles: UserRole[]
    id: string
    lastName: string
}

export interface EmployeeIdentityRequest {
    email: string | null
    externalId: ExternalId
    firstName: string
    lastName: string
}

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

export interface MergeRequest {
    duplicate: string
    master: string
}

export interface PersonWithChildrenDTO {
    addresses: PersonAddressDTO[]
    children: PersonWithChildrenDTO[]
    dateOfBirth: LocalDate
    dateOfDeath: LocalDate | null
    firstName: string
    id: string
    lastName: string
    nationalities: Nationality[]
    nativeLanguage: NativeLanguage | null
    residenceCode: string | null
    restrictedDetails: RestrictedDetails
    socialSecurityNumber: string | null
}

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

export interface PersonIdentityResponseJSON {
    id: string
    socialSecurityNumber: string | null
}

export interface PersonJSON {
    backupPhone: string
    dateOfBirth: LocalDate
    dateOfDeath: LocalDate | null
    email: string | null
    firstName: string | null
    forceManualFeeDecisions: boolean
    id: string
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

export interface AddSsnRequest {
    ssn: string
}

export interface PartnershipUpdateRequest {
    endDate: LocalDate | null
    startDate: LocalDate
}

export interface Partnership {
    conflict: boolean
    endDate: LocalDate | null
    id: string
    partners: PersonJSON[]
    startDate: LocalDate
}

export interface PartnershipRequest {
    endDate: LocalDate | null
    personIds: string[]
    startDate: LocalDate
}

export interface ParentshipUpdateRequest {
    endDate: LocalDate
    startDate: LocalDate
}

export interface Parentship {
    child: PersonJSON
    childId: string
    conflict: boolean
    endDate: LocalDate
    headOfChild: PersonJSON
    headOfChildId: string
    id: string
    startDate: LocalDate
}

export interface ParentshipRequest {
    childId: string
    endDate: LocalDate
    headOfChildId: string
    startDate: LocalDate
}

export interface FamilyContactUpdate {
    childId: string
    contactPersonId: string
    priority: number | null
}

export interface FamilyContact {
    backupPhone: string | null
    email: string | null
    firstName: string | null
    id: string
    lastName: string | null
    phone: string | null
    postOffice: string
    postalCode: string
    priority: number | null
    role: FamilyContactRole
    streetAddress: string
}

export interface FamilyOverview {
    children: FamilyOverviewPerson[]
    headOfFamily: FamilyOverviewPerson
    partner: FamilyOverviewPerson | null
    totalIncome: number | null
    totalIncomeEffect: IncomeEffect
}

export interface PinCode {
    pin: string
}

export interface EmployeeUpdate {
    globalRoles: UserRole[]
}

export interface EmployeeWithDaycareRoles {
    created: Date
    daycareRoles: DaycareRole[]
    email: string | null
    firstName: string
    globalRoles: UserRole[]
    id: string
    lastName: string
    updated: Date | null
}

export interface Employee {
    created: Date
    email: string | null
    externalId: ExternalId | null
    firstName: string
    id: string
    lastName: string
    updated: Date | null
}

export interface NewEmployee {
    email: string | null
    externalId: ExternalId | null
    firstName: string
    lastName: string
    roles: UserRole[]
}

export interface MobileDeviceIdentity {
    id: string
    longTermToken: string
}

export interface PostPairingValidationReq {
    challengeKey: string
    responseKey: string
}

export interface PostPairingResponseReq {
    challengeKey: string
    responseKey: string
}

export interface PostPairingChallengeReq {
    challengeKey: string
}

export interface Pairing {
    challengeKey: string
    expires: Date
    id: string
    mobileDeviceId: string | null
    responseKey: string | null
    status: PairingStatus
    unitId: string
}

export interface PostPairingReq {
    unitId: string
}

export interface PairingStatusRes {
    status: PairingStatus
}

export interface RenameRequest {
    name: string
}

export interface MobileDevice {
    id: string
    name: string
    unitId: string
}

export interface OccupancyResponseGroupLevel {
    groupId: string
    occupancies: OccupancyResponse
}

export interface OccupancyResponse {
    max: OccupancyPeriod | null
    min: OccupancyPeriod | null
    occupancies: OccupancyPeriod[]
}

export interface ReplyToMessageBody {
    content: string
    recipientAccountIds: string[]
}

export interface CitizenMessageBody {
    content: string
    recipients: MessageAccount[]
    title: string
}

export interface MessageAccount {
    id: string
    name: string
}

export interface UpsertableDraftContent {
    content: string
    recipientIds: string[]
    recipientNames: string[]
    title: string
    type: MessageType
}

export interface ThreadReply {
    message: Message
    threadId: string
}

export interface ReplyToMessageBody {
    content: string
    recipientAccountIds: string[]
}

export interface UnreadMessagesResponse {
    count: number
}

export interface SentMessage {
    content: string
    contentId: string
    recipientNames: string[]
    recipients: MessageAccount[]
    sentAt: Date
    threadTitle: string
    type: MessageType
}

export interface MessageReceiversResponse {
    groupId: string
    groupName: string
    receivers: MessageReceiver[]
}

export interface MessageThread {
    id: string
    messages: Message[]
    title: string
    type: MessageType
}

export interface DraftContent {
    content: string
    created: Date
    id: string
    recipientIds: string[]
    recipientNames: string[]
    title: string
    type: MessageType
}

export interface DetailedMessageAccount {
    daycareGroup: Group | null
    id: string
    name: string
    type: AccountType
    unreadCount: number
}

export interface PostMessageBody {
    content: string
    recipientAccountIds: string[]
    recipientNames: string[]
    title: string
    type: MessageType
}

export interface Recipient {
    blocklisted: boolean
    firstName: string
    guardian: boolean
    headOfChild: boolean
    lastName: string
    personId: string
}

export interface EditRecipientRequest {
    blocklisted: boolean
}

export interface DaycareDailyNote {
    childId: string | null
    date: LocalDate
    feedingNote: DaycareDailyNoteLevelInfo | null
    groupId: string | null
    id: string | null
    modifiedAt: Date | null
    modifiedBy: string | null
    note: string | null
    reminderNote: string | null
    reminders: DaycareDailyNoteReminder[]
    sleepingMinutes: number | null
    sleepingNote: DaycareDailyNoteLevelInfo | null
}

export interface VoucherValueDecisionSummary {
    approvedAt: Date | null
    child: Basic
    created: Date
    decisionNumber: number | null
    finalCoPayment: number
    headOfFamily: Basic
    id: string
    sentAt: Date | null
    status: VoucherValueDecisionStatus
    validFrom: LocalDate
    validTo: LocalDate | null
    voucherValue: number
}

export interface InvoicePayload {
    areas: string[]
    dueDate: LocalDate | null
    from: LocalDate
    invoiceDate: LocalDate | null
    to: LocalDate
}

export interface InvoiceSummary {
    account: number
    createdAt: Date | null
    headOfFamily: Detailed
    id: string
    periodEnd: LocalDate
    periodStart: LocalDate
    rows: InvoiceRowSummary[]
    sentAt: Date | null
    sentBy: string | null
    status: InvoiceStatus
}

export interface InvoiceCodes {
    agreementTypes: number[]
    costCenters: string[]
    products: Product[]
    subCostCenters: string[]
}

export interface InvoiceDetailed {
    account: number
    agreementType: number
    dueDate: LocalDate
    headOfFamily: Detailed
    id: string
    invoiceDate: LocalDate
    number: number | null
    periodEnd: LocalDate
    periodStart: LocalDate
    rows: InvoiceRowDetailed[]
    sentAt: Date | null
    sentBy: string | null
    status: InvoiceStatus
}

export interface Invoice {
    agreementType: number
    dueDate: LocalDate
    headOfFamily: JustId
    id: string
    invoiceDate: LocalDate
    number: number | null
    periodEnd: LocalDate
    periodStart: LocalDate
    rows: InvoiceRow[]
    sentAt: Date | null
    sentBy: string | null
    status: InvoiceStatus
}

export interface FeeThresholdsWithId {
    id: string
    thresholds: FeeThresholds
}

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

export interface GenerateDecisionsBody {
    starting: string
    targetHeads: string[]
}

export interface FeeDecisionTypeRequest {
    type: FeeDecisionType
}

export interface FeeDecisionSummary {
    approvedAt: Date | null
    children: Basic[]
    created: Date
    decisionNumber: number | null
    finalPrice: number
    headOfFamily: Basic
    id: string
    sentAt: Date | null
    status: FeeDecisionStatus
    validDuring: DateRange
}

export interface CreateRetroactiveFeeDecisionsBody {
    from: LocalDate
}

export interface FeeAlteration {
    amount: number
    id: string | null
    isAbsolute: boolean
    notes: string
    personId: string
    type: Type
    updatedAt: Date | null
    updatedBy: string | null
    validFrom: LocalDate
    validTo: LocalDate | null
}

export interface DecisionListResponse {
    decisions: Decision[]
}

export interface DecisionUnit {
    daycareDecisionName: string
    decisionHandler: string
    decisionHandlerAddress: string
    id: string
    manager: string | null
    name: string
    phone: string | null
    postOffice: string
    postalCode: string
    preschoolDecisionName: string
    providerType: ProviderType
    streetAddress: string
}

export interface GroupAclUpdate {
    groupIds: string[]
}

export interface DaycareAclResponse {
    rows: DaycareAclRow[]
}

export interface PreschoolTerm {
    applicationPeriod: FiniteDateRange
    extendedTerm: FiniteDateRange
    finnishPreschool: FiniteDateRange
    swedishPreschool: FiniteDateRange
}

export interface ClubTerm {
    applicationPeriod: FiniteDateRange
    term: FiniteDateRange
}

export interface StaffAttendanceUpdate {
    count: number | null
    countOther: number | null
    date: LocalDate
    groupId: string
}

export interface UnitStaffAttendance {
    count: number
    countOther: number
    date: LocalDate
    groups: GroupStaffAttendance[]
    updated: Date | null
}

export interface UnitStub {
    id: string
    name: string
}

export interface PublicUnit {
    clubApplyPeriod: DateRange | null
    daycareApplyPeriod: DateRange | null
    email: string | null
    ghostUnit: boolean | null
    id: string
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

export interface GroupUpdateRequest {
    endDate: LocalDate | null
    name: string
    startDate: LocalDate
}

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
    id: string
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

export interface CaretakersResponse {
    caretakers: CaretakerAmount[]
    groupName: string
    unitName: string
}

export interface DaycareGroup {
    daycareId: string
    deletable: boolean
    endDate: LocalDate | null
    id: string
    name: string
    startDate: LocalDate
}

export interface CreateGroupRequest {
    initialCaretakers: number
    name: string
    startDate: LocalDate
}

export interface CreateDaycareResponse {
    id: string
}

export interface DaycareFields {
    additionalInfo: string | null
    areaId: string
    capacity: number
    closingDate: LocalDate | null
    clubApplyPeriod: DateRange | null
    costCenter: string | null
    daycareApplyPeriod: DateRange | null
    decisionCustomization: DaycareDecisionCustomization
    email: string | null
    financeDecisionHandlerId: string | null
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

export interface CaretakerRequest {
    amount: number
    endDate: LocalDate | null
    startDate: LocalDate
}

export interface AdditionalInformation {
    additionalInfo: string
    allergies: string
    diet: string
    medication: string
    preferredName: string
}

export interface Absence {
    absenceType: AbsenceType
    careType: AbsenceCareType
    childId: string
    date: LocalDate
    id: string | null
    modifiedAt: string | null
    modifiedBy: string | null
}

export interface AbsenceDelete {
    careType: AbsenceCareType
    childId: string
    date: LocalDate
}

export interface DailyServiceTimesResponse {
    dailyServiceTimes: DailyServiceTimes | null
}

export interface ChildBackupPickup {
    childId: string
    id: string
    name: string
    phone: string
}

export interface ChildBackupPickupCreateResponse {
    id: string
}

export interface ChildBackupPickupContent {
    name: string
    phone: string
}

export interface BackupCareUpdateRequest {
    groupId: string | null
    period: FiniteDateRange
}

export interface UnitBackupCaresResponse {
    backupCares: UnitBackupCare[]
}

export interface ChildBackupCaresResponse {
    backupCares: ChildBackupCare[]
}

export interface BackupCareCreateResponse {
    id: string
}

export interface NewBackupCare {
    groupId: string | null
    period: FiniteDateRange
    unitId: string
}

export interface FullDayAbsenceRequest {
    absenceType: AbsenceType
}

export interface DepartureRequest {
    absenceType: AbsenceType | null
    departed: string
}

export interface ArrivalRequest {
    arrived: string
}

export interface AbsenceRangeRequest {
    absenceType: AbsenceType
    endDate: LocalDate
    startDate: LocalDate
}

export interface ChildResult {
    child: ChildSensitiveInformation | null
    status: ChildResultStatus
}

export interface GetChildSensitiveInfoRequest {
    pin: string
    staffId: string
}

export interface DepartureInfoResponse {
    absentFrom: AbsenceCareType[]
}

export interface AttendanceResponse {
    children: Child[]
    unit: UnitInfo
}

export interface AssistanceBasisOption {
    descriptionFi: string | null
    nameFi: string
    value: string
}

export interface AssistanceNeed {
    bases: string[]
    capacityFactor: number
    childId: string
    description: string
    endDate: LocalDate
    id: string
    otherBasis: string
    startDate: LocalDate
}

export interface AssistanceNeedRequest {
    bases: string[]
    capacityFactor: number
    description: string
    endDate: LocalDate
    otherBasis: string
    startDate: LocalDate
}

export interface AssistanceActionOption {
    nameFi: string
    value: string
}

export interface AssistanceAction {
    actions: string[]
    childId: string
    endDate: LocalDate
    id: string
    measures: AssistanceMeasure[]
    otherAction: string
    startDate: LocalDate
}

export interface AssistanceActionRequest {
    actions: string[]
    endDate: LocalDate
    measures: AssistanceMeasure[]
    otherAction: string
    startDate: LocalDate
}

export interface NotesWrapperJSON {
    notes: NoteJSON[]
}

export interface NoteSearchDTO {
    applicationIds: string[]
}

export interface NoteJSON {
    applicationId: string
    created: Date
    createdBy: string
    createdByName: string
    id: string
    text: string
    updated: Date
    updatedBy: string | null
    updatedByName: string | null
}

export interface NoteRequest {
    text: string
}

export interface DecisionDraftUpdate {
    endDate: LocalDate
    id: string
    planned: boolean
    startDate: LocalDate
    unitId: string
}

export interface ApplicationUpdate {
    dueDate: LocalDate | null
    form: ApplicationFormUpdate
}

export interface SimpleBatchRequest {
    applicationIds: string[]
}

export interface PlacementProposalConfirmationUpdate {
    otherReason: string | null
    reason: PlacementPlanRejectReason | null
    status: PlacementPlanConfirmationStatus
}

export interface PlacementPlanDraft {
    child: PlacementDraftChild
    guardianHasRestrictedDetails: boolean
    period: FiniteDateRange
    placements: PlacementDraftPlacement[]
    preferredUnits: PlacementDraftUnit[]
    preschoolDaycarePeriod: FiniteDateRange | null
    type: PlacementType
}

export interface DecisionDraftJSON {
    child: ChildInfo
    decisions: DecisionDraft[]
    guardian: GuardianInfo
    otherGuardian: GuardianInfo | null
    placementUnitName: string
    unit: DecisionUnit
}

export interface PersonApplicationSummary {
    applicationId: string
    childId: string
    childName: string | null
    childSsn: string | null
    connectedDaycare: boolean
    guardianId: string
    guardianName: string
    preferredStartDate: LocalDate | null
    preferredUnitId: string | null
    preferredUnitName: string | null
    preparatoryEducation: boolean
    sentDate: LocalDate | null
    status: ApplicationStatus
    type: string
}

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
    id: string
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

export interface ApplicationResponse {
    application: ApplicationDetails
    attachments: Attachment[]
    decisions: Decision[]
    guardians: PersonJSON[]
}

export interface DaycarePlacementPlan {
    period: FiniteDateRange
    preschoolDaycarePeriod: FiniteDateRange | null
    unitId: string
}

export interface PaperApplicationCreateRequest {
    childId: string
    guardianId: string | null
    guardianSsn: string | null
    guardianToBeCreated: CreatePersonBody | null
    hideFromGuardian: boolean
    sentDate: LocalDate
    transferApplication: boolean
    type: ApplicationType
}

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

export interface RejectDecisionRequest {
    decisionId: string
}

export interface ApplicationsOfChild {
    applicationSummaries: CitizenApplicationSummary[]
    childId: string
    childName: string
}

export interface ApplicationDecisions {
    applicationId: string
    childName: string
    decisions: DecisionSummary[]
}

export interface Decision {
    applicationId: string
    childId: string
    childName: string
    createdBy: string
    decisionNumber: number
    documentKey: string | null
    endDate: LocalDate
    id: string
    otherGuardianDocumentKey: string | null
    requestedStartDate: LocalDate | null
    resolved: LocalDate | null
    sentDate: LocalDate
    startDate: LocalDate
    status: DecisionStatus
    type: DecisionType
    unit: DecisionUnit
}

export interface ApplicationDetails {
    additionalDaycareApplication: boolean
    attachments: Attachment[]
    checkedByAdmin: boolean
    childId: string
    childRestricted: boolean
    createdDate: Date | null
    dueDate: LocalDate | null
    dueDateSetManuallyAt: Date | null
    form: ApplicationForm
    guardianDateOfDeath: LocalDate | null
    guardianId: string
    guardianRestricted: boolean
    hideFromGuardian: boolean
    id: string
    modifiedDate: Date | null
    origin: ApplicationOrigin
    otherGuardianId: string | null
    otherGuardianLivesInSameAddress: boolean | null
    sentDate: LocalDate | null
    status: ApplicationStatus
    transferApplication: boolean
    type: ApplicationType
}

export interface CreateApplicationBody {
    childId: string
    type: ApplicationType
}

export interface AcceptDecisionRequest {
    decisionId: string
    requestedStartDate: LocalDate
}