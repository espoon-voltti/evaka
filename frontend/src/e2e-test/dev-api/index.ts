// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as fs from 'fs/promises'

import axios, {
  AxiosHeaders,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios'
import FormData from 'form-data'
import { BaseError } from 'make-error-cause'

import { VoucherValueDecision } from 'e2e-test/generated/api-types'
import {
  ApplicationDetails,
  deserializeApplicationDetails
} from 'lib-common/api-types/application/ApplicationDetails'
import { ScopedRole } from 'lib-common/api-types/employee-auth'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import {
  AssistanceFactor,
  DaycareAssistance,
  OtherAssistanceMeasure,
  PreschoolAssistance
} from 'lib-common/generated/api-types/assistance'
import { StaffMemberAttendance } from 'lib-common/generated/api-types/attendance'
import { ClubTerm } from 'lib-common/generated/api-types/daycare'
import {
  FeeDecision,
  FeeThresholds,
  IncomeNotification,
  Invoice
} from 'lib-common/generated/api-types/invoicing'
import {
  ChildDailyNoteBody,
  GroupNoteBody
} from 'lib-common/generated/api-types/note'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import {
  CurriculumType,
  VasuLanguage
} from 'lib-common/generated/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../config'

import {
  Application,
  AssistanceNeedDecision,
  BackupCare,
  BackupPickup,
  CareArea,
  Child,
  ChildAttendance,
  Daycare,
  DaycareCaretakers,
  DaycareGroup,
  DaycareGroupPlacement,
  DaycarePlacement,
  Decision,
  DecisionFixture,
  deserializeDecision,
  DevAbsence,
  DevAssistanceNeedPreschoolDecision,
  DevCalendarEvent,
  DevCalendarEventAttendee,
  DevChildDocument,
  DevDailyServiceTime,
  DevDailyServiceTimeNotification,
  DevDocumentTemplate,
  DevFixedPeriodQuestionnaire,
  DevHoliday,
  DevHolidayPeriod,
  DevIncome,
  DevPayment,
  DevPreschoolTerm,
  DevRealtimeStaffAttendance,
  DevStaffAttendancePlan,
  DevVardaReset,
  DevVardaServiceNeed,
  Email,
  EmployeeDetail,
  EmployeePin,
  FamilyContact,
  FosterParent,
  FridgeChild,
  FridgePartner,
  PedagogicalDocument,
  PersonDetail,
  PersonDetailWithDependantsAndGuardians,
  PlacementPlan,
  ServiceNeedFixture,
  SuomiFiMessage,
  VtjPerson
} from './types'

export class DevApiError extends BaseError {
  constructor(cause: unknown) {
    if (axios.isAxiosError(cause)) {
      const message = `${cause.message}: ${formatRequest(
        cause.config
      )}\n${formatResponse(cause.response)}`
      super(message, cause)
    } else if (cause instanceof Error) {
      super('Dev API error', cause)
    } else {
      super('Dev API error')
    }
  }
}

function clockHeader(now: HelsinkiDateTime): AxiosHeaders {
  return new AxiosHeaders({
    EvakaMockedTime: now.formatIso()
  })
}

function formatRequest(config: InternalAxiosRequestConfig | undefined): string {
  if (!config || !config.url) return ''
  const url = new URL(config.url, config.baseURL)
  return `${String(config.method)} ${url.pathname}${url.search}${url.hash}`
}

function formatResponse(response: AxiosResponse | undefined): string {
  if (!response) return '[no response]'
  if (!response.data) return '[no response body]'
  if (typeof response.data === 'string') return response.data
  return JSON.stringify(response.data, null, 2)
}

export const devClient = axios.create({
  baseURL: config.devApiGwUrl
})

export async function setTestMode(enabled: boolean): Promise<void> {
  try {
    await devClient.post('/test-mode', {}, { params: { enabled } })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertApplications(
  fixture: Application[]
): Promise<void> {
  try {
    await devClient.post(`/applications`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

type ApplicationActionSimple =
  | 'move-to-waiting-placement'
  | 'cancel-application'
  | 'set-verified'
  | 'set-unverified'
  | 'create-default-placement-plan'
  | 'confirm-placement-without-decision'
  | 'send-decisions-without-proposal'
  | 'send-placement-proposal'
  | 'confirm-decision-mailed'

export async function execSimpleApplicationAction(
  applicationId: string,
  action: ApplicationActionSimple,
  mockedTime: HelsinkiDateTime
): Promise<void> {
  try {
    await devClient.post(
      `/applications/${applicationId}/actions/${action}`,
      null,
      {
        headers: clockHeader(mockedTime)
      }
    )
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function createPlacementPlan(
  applicationId: string,
  body: { unitId: string; period: { start: string; end: string } }
): Promise<void> {
  try {
    await devClient.post(
      `/applications/${applicationId}/actions/create-placement-plan`,
      body
    )
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function execSimpleApplicationActions(
  applicationId: string,
  actions: ApplicationActionSimple[],
  mockedTime: HelsinkiDateTime
): Promise<void> {
  for (const action of actions) {
    await execSimpleApplicationAction(applicationId, action, mockedTime)
    if (action === 'move-to-waiting-placement') {
      await runPendingAsyncJobs(mockedTime)
    }
  }
}

export async function insertPlacementPlan(
  fixture: PlacementPlan
): Promise<void> {
  try {
    await devClient.post(`/placement-plan/${fixture.applicationId}`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertHolidayPeriod({
  id,
  ...rest
}: DevHolidayPeriod): Promise<void> {
  try {
    await devClient.post(`/holiday-period/${id}`, rest)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertHolidayQuestionnaire({
  id,
  ...rest
}: DevFixedPeriodQuestionnaire): Promise<void> {
  try {
    await devClient.post(`/holiday-period/questionnaire/${id}`, rest)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertHoliday(holiday: DevHoliday): Promise<void> {
  try {
    await devClient.post(`/holiday`, holiday)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertCareAreaFixtures(
  fixture: CareArea[]
): Promise<void> {
  try {
    await devClient.post(`/care-areas`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDaycareFixtures(fixture: Daycare[]): Promise<void> {
  try {
    await devClient.post(
      `/daycares`,
      fixture.map(
        ({
          streetAddress,
          postalCode,
          postOffice,
          decisionDaycareName,
          decisionPreschoolName,
          decisionHandler,
          decisionHandlerAddress,
          ...daycare
        }) => ({
          ...daycare,
          visitingAddress: {
            streetAddress,
            postalCode,
            postOffice
          },
          decisionCustomization: {
            daycareName: decisionDaycareName,
            preschoolName: decisionPreschoolName,
            handler: decisionHandler,
            handlerAddress: decisionHandlerAddress
          }
        })
      )
    )
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function deleteDaycareCostCenter(
  daycareId: string
): Promise<void> {
  try {
    await devClient.delete(`/daycare/${daycareId}/cost-center`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDaycareGroupFixtures(
  fixture: DaycareGroup[]
): Promise<void> {
  try {
    await devClient.post(`/daycare-groups`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertChildFixtures(fixture: Child[]): Promise<void> {
  try {
    await devClient.post(`/children`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertAssistanceFactorFixtures(
  fixture: AssistanceFactor[]
): Promise<void> {
  try {
    await devClient.post(`/assistance-factors`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDaycareAssistanceFixtures(
  fixture: DaycareAssistance[]
): Promise<void> {
  try {
    await devClient.post(`/daycare-assistances`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertPreschoolAssistanceFixtures(
  fixture: PreschoolAssistance[]
): Promise<void> {
  try {
    await devClient.post(`/preschool-assistances`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertOtherAssistanceMeasureFixtures(
  fixture: OtherAssistanceMeasure[]
): Promise<void> {
  try {
    await devClient.post(`/other-assistance-measures`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertAssistanceNeedDecisionFixtures(
  fixture: AssistanceNeedDecision[]
): Promise<void> {
  try {
    await devClient.post(`/assistance-need-decisions`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertAssistanceNeedPreschoolDecisionFixtures(
  fixture: DevAssistanceNeedPreschoolDecision[]
): Promise<void> {
  try {
    await devClient.post(`/assistance-need-preschool-decisions`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export function insertDaycareCaretakerFixtures(
  fixtures: DaycareCaretakers[]
): Promise<void> {
  try {
    return devClient.post(`/daycare-caretakers`, fixtures)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export function insertChildAttendanceFixtures(
  fixtures: ChildAttendance[]
): Promise<void> {
  try {
    return devClient.post(`/attendances`, fixtures)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export function insertReservationFixtures(
  fixtures: DailyReservationRequest[]
): Promise<void> {
  try {
    return devClient.post(`/reservations`, fixtures)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export function insertAbsenceFixture(fixture: DevAbsence): Promise<void> {
  try {
    return devClient.post(`/absence`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDaycarePlacementFixtures(
  fixture: DaycarePlacement[]
): Promise<void> {
  try {
    await devClient.post(`/daycare-placements`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDaycareGroupPlacementFixtures(
  fixture: DaycareGroupPlacement[]
): Promise<void> {
  try {
    await devClient.post(`/daycare-group-placements`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertBackupCareFixtures(
  fixture: BackupCare[]
): Promise<void> {
  try {
    await devClient.post(`/backup-cares`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertParentshipFixtures(
  fixtures: {
    childId: UUID
    headOfChildId: UUID
    startDate: LocalDate
    endDate: LocalDate | null
  }[]
): Promise<void> {
  try {
    await devClient.post(`/parentship`, fixtures)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function upsertMessageAccounts(): Promise<void> {
  try {
    await devClient.post(`/message-account/upsert-all`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function setAclForDaycares(
  externalId: string,
  daycareId: UUID,
  role: ScopedRole = 'UNIT_SUPERVISOR'
): Promise<void> {
  try {
    await devClient.put(`/daycares/${daycareId}/acl`, { externalId, role })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function setAclForDaycareGroups(
  employeeId: UUID,
  groupIds: UUID[]
): Promise<void> {
  try {
    await devClient.post(
      `/daycare-group-acl`,
      groupIds.map((groupId) => ({ groupId, employeeId }))
    )
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function resetDatabase(): Promise<void> {
  try {
    await devClient.post(`/reset-db`, null)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDecisionFixtures(
  fixture: DecisionFixture[]
): Promise<void> {
  try {
    await devClient.post(`/decisions`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function createDecisionPdf(id: string): Promise<void> {
  try {
    await devClient.post(`/decisions/${id}/actions/create-pdf`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function rejectDecisionByCitizen(id: string): Promise<void> {
  try {
    await devClient.post(`/decisions/${id}/actions/reject-by-citizen`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertFeeDecisionFixtures(
  fixture: FeeDecision[]
): Promise<void> {
  try {
    await devClient.post(`/fee-decisions`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertVoucherValueDecisionFixtures(
  fixture: VoucherValueDecision[]
): Promise<void> {
  try {
    await devClient.post(`/value-decisions`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertInvoiceFixtures(fixture: Invoice[]): Promise<void> {
  try {
    await devClient.post(`/invoices`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertFeeThresholds(
  fixture: FeeThresholds
): Promise<void> {
  try {
    await devClient.post('/fee-thresholds', fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export interface IncomeStatementFixture {
  type: string
  startDate: LocalDate
  endDate: LocalDate | null
  otherInfo?: string
  attachmentIds?: UUID[]
}

export async function insertIncomeStatements(
  personId: UUID,
  data: IncomeStatementFixture[]
): Promise<void> {
  try {
    await devClient.post(`/income-statements`, { personId, data })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertIncome(fixture: DevIncome) {
  try {
    await devClient.post('/income', fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertIncomeNotification(fixture: IncomeNotification) {
  try {
    await devClient.post('/income-notifications', fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

// returns the id of the upserted person
export async function upsertPersonFixture(
  fixture: PersonDetail
): Promise<string> {
  try {
    const { data: id } = await devClient.post<UUID>(`/person`, fixture)
    return id
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertEmployeeFixture(
  fixture: EmployeeDetail
): Promise<string> {
  try {
    const { data } = await devClient.post<UUID>(`/employee`, fixture)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertPersonFixture(
  fixture: PersonDetail
): Promise<string> {
  try {
    const { data } = await devClient.post<UUID>(`/person/create`, fixture)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertChildFixture(
  fixture: PersonDetail
): Promise<string> {
  try {
    const { data } = await devClient.post<UUID>(`/child`, fixture)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertServiceNeeds(
  serviceNeeds: ServiceNeedFixture[]
): Promise<void> {
  try {
    await devClient.post<UUID>(`/service-need`, serviceNeeds)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertServiceNeedOptions(
  serviceNeedOptions: ServiceNeedOption[]
): Promise<void> {
  try {
    await devClient.post<UUID>(`/service-need-option`, serviceNeedOptions)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDefaultServiceNeedOptions(): Promise<void> {
  try {
    await devClient.post<UUID>(`/service-need-options`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertVoucherValues(): Promise<void> {
  try {
    await devClient.post<UUID>(`/voucher-values`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDailyServiceTime(
  data: DevDailyServiceTime
): Promise<void> {
  try {
    await devClient.post('/daily-service-time', data)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDailyServiceTimeNotification(
  data: DevDailyServiceTimeNotification
): Promise<void> {
  try {
    await devClient.post('/daily-service-time-notification', data)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function runPendingAsyncJobs(
  mockedTime: HelsinkiDateTime
): Promise<void> {
  try {
    await devClient.post(`/run-jobs`, null, {
      headers: clockHeader(mockedTime)
    })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function getMessages(): Promise<SuomiFiMessage[]> {
  try {
    return (await devClient.get<SuomiFiMessage[]>(`/messages`)).data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function cleanUpMessages(): Promise<void> {
  try {
    await devClient.post(`/messages/clean-up`, null)
  } catch (e) {
    throw new DevApiError(e)
  }
}

const toVtjPerson = (
  person: PersonDetailWithDependantsAndGuardians
): VtjPerson => ({
  firstNames: person.firstName,
  lastName: person.lastName,
  socialSecurityNumber: person.ssn || '',
  address: {
    streetAddress: person.streetAddress || '',
    postalCode: person.postalCode || '',
    postOffice: person.postOffice || '',
    streetAddressSe: person.streetAddress || '',
    postOfficeSe: person.postalCode || ''
  },
  dependants: person.dependants?.map(toVtjPerson) ?? [],
  guardians: person.guardians?.map(toVtjPerson) ?? [],
  dateOfDeath: person.dateOfDeath ?? null,
  nationalities: [],
  nativeLanguage: null,
  residenceCode:
    person.residenceCode ??
    `${person.streetAddress ?? ''}${person.postalCode ?? ''}${
      person.postOffice ?? ''
    }`.replace(' ', ''),
  restrictedDetails: {
    enabled: person.restrictedDetailsEnabled || false,
    endDate: person.restrictedDetailsEndDate || null
  }
})

export async function insertVtjPersonFixture(
  fixture: PersonDetailWithDependantsAndGuardians
): Promise<void> {
  const vtjPerson = toVtjPerson(fixture)
  try {
    await devClient.post<VtjPerson>(`/vtj-persons`, vtjPerson)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function getVtjPerson(ssn: string): Promise<VtjPerson> {
  try {
    const { data } = await devClient.get<VtjPerson>(`/vtj-persons/${ssn}`)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export function personToVtjPerson(
  person: PersonDetail,
  dependants: PersonDetail[],
  guardians: PersonDetail[]
): VtjPerson {
  return {
    address: {
      streetAddress: person.streetAddress || null,
      postalCode: person.postalCode || null,
      postOffice: person.postOffice || null,
      streetAddressSe: null,
      postOfficeSe: null
    },
    dateOfDeath: null,
    dependants: dependants.map((dependant) =>
      personToVtjPerson(dependant, [], [])
    ),
    firstNames: person.firstName,
    guardians: guardians.map((guardian) => personToVtjPerson(guardian, [], [])),
    lastName: person.lastName,
    nationalities: [
      {
        countryName: 'Suomi',
        countryCode: 'FIN'
      }
    ],
    nativeLanguage: {
      languageName: 'suomi',
      code: 'fi'
    },
    residenceCode: null,
    restrictedDetails: null,
    socialSecurityNumber: person.ssn || 'e2e-fixture-ssn-missing'
  }
}

export async function getSentEmails(): Promise<Email[]> {
  try {
    const { data } = await devClient.get<Email[]>(`/emails`)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

type PairingStatus =
  | 'WAITING_CHALLENGE'
  | 'WAITING_RESPONSE'
  | 'READY'
  | 'PAIRED'

export interface PairingResponse {
  id: UUID
  unitId: UUID
  challengeKey: string
  responseKey: string | null
  expires: Date
  status: PairingStatus
  mobileDeviceId: UUID | null
}

export async function postPairingChallenge(
  challengeKey: string
): Promise<PairingResponse> {
  try {
    const { data } = await devClient.post<PairingResponse>(
      `/mobile/pairings/challenge`,
      {
        challengeKey
      }
    )
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function postPairingResponse(
  pairingId: UUID,
  challengeKey: string,
  responseKey: string
): Promise<PairingResponse> {
  try {
    const { data } = await devClient.post<PairingResponse>(
      `/mobile/pairings/${pairingId}/response`,
      {
        challengeKey,
        responseKey
      }
    )
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function postPairing(unitId: UUID): Promise<PairingResponse> {
  try {
    const { data } = await devClient.post<PairingResponse>(`/mobile/pairings`, {
      unitId
    })
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

interface MobileDevice {
  id: UUID
  unitId: UUID
  name: string
  longTermToken: UUID
}

export async function postMobileDevice(
  mobileDevice: MobileDevice
): Promise<void> {
  try {
    await devClient.post<PairingResponse>(`/mobile/devices`, mobileDevice)
  } catch (e) {
    throw new DevApiError(e)
  }
}

interface PersonalMobileDevice {
  id: UUID
  employeeId: UUID
  name: string
  longTermToken: UUID
}

export async function postPersonalMobileDevice(
  device: PersonalMobileDevice
): Promise<void> {
  try {
    await devClient.post<PairingResponse>(`/mobile/personal-devices`, device)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function getDecisionsByApplication(
  applicationId: string
): Promise<Decision[]> {
  try {
    const { data } = await devClient.get<JsonOf<Decision[]>>(
      `/applications/${applicationId}/decisions`
    )
    return data.map(deserializeDecision)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function getApplication(
  applicationId: string
): Promise<ApplicationDetails> {
  try {
    const { data } = await devClient.get<JsonOf<ApplicationDetails>>(
      `/applications/${applicationId}`
    )
    return deserializeApplicationDetails(data)
  } catch (e) {
    throw new DevApiError(e)
  }
}

interface DigitransitAutocomplete {
  features: DigitransitFeature[]
}

export interface DigitransitFeature {
  geometry: {
    coordinates: [number, number]
  }
  properties: {
    name: string
    postalcode?: string
    locality?: string
    localadmin?: string
  }
}

export async function putDigitransitAutocomplete(
  mockResponse: DigitransitAutocomplete
): Promise<void> {
  try {
    await devClient.put('/digitransit/autocomplete', mockResponse)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function postChildDailyNote(
  childId: UUID,
  note: ChildDailyNoteBody
): Promise<void> {
  try {
    await devClient.post(`/children/${childId}/child-daily-notes`, note)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function postGroupNote(
  groupId: UUID,
  note: GroupNoteBody
): Promise<void> {
  try {
    await devClient.post(`/daycare-groups/${groupId}/group-notes`, note)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertFamilyContacts(
  fixture: FamilyContact[]
): Promise<void> {
  try {
    await devClient.post(`/family-contact`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertBackupPickups(
  fixture: BackupPickup[]
): Promise<void> {
  try {
    await devClient.post(`/backup-pickup`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertFridgeChildren(
  fixture: FridgeChild[]
): Promise<void> {
  try {
    await devClient.post(`/fridge-child`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertFridgePartners(
  fixture: FridgePartner[]
): Promise<void> {
  try {
    await devClient.post(`/fridge-partner`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertFosterParents(fixtures: FosterParent[]) {
  try {
    await devClient.post(`/foster-parent`, fixtures)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertEmployeePins(
  fixture: EmployeePin[]
): Promise<void> {
  try {
    await devClient.post(`/employee-pin`, fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertPedagogicalDocuments(
  fixture: PedagogicalDocument[]
): Promise<void> {
  try {
    await devClient.post('/pedagogical-document', fixture)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertPedagogicalDocumentAttachment(
  pedagogicalDocumentId: string,
  employeeId: string,
  fileName: string,
  filePath: string
): Promise<string> {
  const file = await fs.readFile(`${filePath}/${fileName}`)
  const form = new FormData()
  form.append('file', file, fileName)
  form.append('employeeId', employeeId)
  try {
    return await devClient
      .post<string>(
        `/pedagogical-document-attachment/${pedagogicalDocumentId}`,
        form,
        { headers: form.getHeaders() }
      )
      .then((res) => res.data)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertGuardianFixtures(
  guardians: { guardianId: string; childId: string }[]
): Promise<void> {
  try {
    await devClient.post('/guardian', guardians)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertVasuTemplateFixture(
  body: Partial<{
    name: string
    valid: FiniteDateRange
    type: CurriculumType
    language: VasuLanguage
  }> = {}
): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>('/vasu/template', body)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function deleteVasuTemplates(): Promise<void> {
  try {
    await devClient.delete<void>('/vasu/templates')
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertVasuDocument(
  childId: UUID,
  templateId: UUID
): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>('/vasu/doc', {
      childId,
      templateId
    })
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function publishVasuDocument(documentId: UUID): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>(
      `/vasu/doc/publish/${documentId}`
    )
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertDocumentTemplateFixture(
  fixture: DevDocumentTemplate
): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>('/document-templates', fixture)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertChildDocumentFixture(
  fixture: DevChildDocument
): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>('/child-documents', fixture)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function terminatePlacement(
  placementId: string,
  endDate: LocalDate,
  terminationRequestedDate: LocalDate | null,
  terminatedBy: string | null
): Promise<void> {
  try {
    await devClient.post('/placement/terminate', {
      placementId,
      endDate,
      terminationRequestedDate,
      terminatedBy
    })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function addVardaReset(vardaReset: DevVardaReset): Promise<void> {
  try {
    await devClient.post('/varda/reset-child', vardaReset)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function addVardaServiceNeed(
  vardaServiceNeed: DevVardaServiceNeed
): Promise<void> {
  try {
    await devClient.post('/varda/varda-service-need', vardaServiceNeed)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function upsertOccupancyCoefficient(body: {
  coefficient: number
  employeeId: UUID
  unitId: UUID
}): Promise<void> {
  try {
    await devClient.post('/occupancy-coefficient', body)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function getStaffRealtimeAttendances(): Promise<
  StaffMemberAttendance[]
> {
  try {
    const { data } = await devClient.get<JsonOf<StaffMemberAttendance[]>>(
      '/realtime-staff-attendance'
    )
    return data.map((row) => ({
      ...row,
      arrived: HelsinkiDateTime.parseIso(row.arrived),
      departed: HelsinkiDateTime.parseNullableIso(row.departed)
    }))
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertStaffRealtimeAttendance(
  body: DevRealtimeStaffAttendance
): Promise<void> {
  try {
    await devClient.post('/realtime-staff-attendance', body)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertStaffAttendancePlan(
  body: DevStaffAttendancePlan
): Promise<void> {
  try {
    await devClient.post('/staff-attendance-plan', body)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function revokeVasuSharingPermission(docId: UUID): Promise<void> {
  try {
    await devClient.post(`/vasu/revokeSharingPermission/${docId}`)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertPayment(body: DevPayment): Promise<void> {
  try {
    await devClient.post('/payments', body)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertCalendarEvent(
  body: DevCalendarEvent
): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>('/calendar-event', body)
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertCalendarEventAttendee(
  body: DevCalendarEventAttendee
): Promise<UUID> {
  try {
    const { data } = await devClient.post<UUID>(
      '/calendar-event-attendee',
      body
    )
    return data
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertAbsence(
  childId: UUID,
  absenceType: AbsenceType,
  date: LocalDate,
  absenceCategory: AbsenceCategory,
  modifiedBy: UUID
): Promise<void> {
  try {
    await devClient.post<void>('/absence', {
      childId,
      absenceType,
      date,
      absenceCategory,
      modifiedBy
    })
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertClubTerm(body: ClubTerm): Promise<void> {
  try {
    await devClient.post<void>('/club-term', body)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function insertPreschoolTerm(
  body: DevPreschoolTerm
): Promise<void> {
  try {
    await devClient.post<void>('/preschool-term', body)
  } catch (e) {
    throw new DevApiError(e)
  }
}

export async function forceFullVtjRefresh(person: UUID): Promise<void> {
  try {
    await devClient.post<void>(
      `/persons/${encodeURIComponent(person)}/force-full-vtj-refresh`
    )
  } catch (e) {
    throw new DevApiError(e)
  }
}
