// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api/client'
import {
  Coordinate,
  DaycareDailyNote,
  DaycareDailyNoteLevelInfo,
  DaycareDailyNoteReminder,
  DaycareGroup,
  DaycarePlacement,
  DaycarePlacementPlan,
  DecisionCustomization,
  MailingAddress,
  Occupancy,
  OccupancyType,
  ProviderType,
  Stats,
  Unit,
  UnitLanguage,
  UnitManager,
  UnitTypes,
  VisitingAddress
} from '../types/unit'
import { UnitBackupCare } from '../types/child'
import { AdRole, DayOfWeek, UUID } from '../types'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { PlacementType } from '../types/child'
import FiniteDateRange from 'lib-common/finite-date-range'
import DateRange from 'lib-common/date-range'
import { ApplicationStatus } from 'lib-common/api-types/application/enums'

function convertUnitJson(unit: JsonOf<Unit>): Unit {
  return {
    ...unit,
    financeDecisionHandler: unit.financeDecisionHandler
      ? {
          id: unit.financeDecisionHandler.id,
          firstName: unit.financeDecisionHandler.firstName,
          lastName: unit.financeDecisionHandler.lastName
        }
      : null,
    openingDate: unit.openingDate ? LocalDate.parseIso(unit.openingDate) : null,
    closingDate: unit.closingDate ? LocalDate.parseIso(unit.closingDate) : null,
    daycareApplyPeriod: unit.daycareApplyPeriod
      ? DateRange.parseJson(unit.daycareApplyPeriod)
      : null,
    preschoolApplyPeriod: unit.preschoolApplyPeriod
      ? DateRange.parseJson(unit.preschoolApplyPeriod)
      : null,
    clubApplyPeriod: unit.clubApplyPeriod
      ? DateRange.parseJson(unit.clubApplyPeriod)
      : null
  }
}

export async function getDaycares(): Promise<Result<Unit[]>> {
  return client
    .get<JsonOf<Unit[]>>('/daycares')
    .then(({ data }) => Success.of(data.map(convertUnitJson)))
    .catch((e) => Failure.fromError(e))
}

export interface DaycareGroupSummary {
  id: UUID
  name: string
}

export interface UnitResponse {
  daycare: Unit
  groups: DaycareGroupSummary[]
  currentUserRoles: AdRole[]
}

export async function getDaycare(id: UUID): Promise<Result<UnitResponse>> {
  return client
    .get<JsonOf<UnitResponse>>(`/daycares/${id}`)
    .then(({ data }) =>
      Success.of({ ...data, daycare: convertUnitJson(data.daycare) })
    )
    .catch((e) => Failure.fromError(e))
}

export type UnitOccupancies = {
  planned: OccupancyResponse
  confirmed: OccupancyResponse
  realized: OccupancyResponse
}

export type GroupOccupancies = {
  confirmed: Record<UUID, OccupancyResponse>
  realized: Record<UUID, OccupancyResponse>
}

export type Caretakers = {
  unitCaretakers: Stats
  groupCaretakers: Record<UUID, Stats>
}

interface MissingGroupPlacementCommon {
  placementId: UUID
  placementPeriod: FiniteDateRange
  childId: UUID
  firstName: string | null
  lastName: string | null
  dateOfBirth: LocalDate
  gap: FiniteDateRange
}

interface MissingGroupPlacementStandard extends MissingGroupPlacementCommon {
  placementType: PlacementType
  backup: false
}

interface MissingGroupPlacementBackupCare extends MissingGroupPlacementCommon {
  placementType: null
  backup: true
}

export type MissingGroupPlacement =
  | MissingGroupPlacementStandard
  | MissingGroupPlacementBackupCare

export type ApplicationUnitSummary = {
  applicationId: UUID
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  guardianFirstName: string
  guardianLastName: string
  guardianPhone: string | null
  guardianEmail: string | null
  requestedPlacementType: PlacementType
  preferredStartDate: LocalDate
  preferenceOrder: number
  status: ApplicationStatus
}

export type UnitData = {
  groups: DaycareGroup[]
  placements: DaycarePlacement[]
  backupCares: UnitBackupCare[]
  caretakers: Caretakers
  unitOccupancies?: UnitOccupancies
  groupOccupancies?: GroupOccupancies
  missingGroupPlacements: MissingGroupPlacement[]
  placementProposals?: DaycarePlacementPlan[]
  placementPlans?: DaycarePlacementPlan[]
  applications?: ApplicationUnitSummary[]
}

export async function getUnitData(
  id: UUID,
  from: LocalDate,
  to: LocalDate
): Promise<Result<UnitData>> {
  try {
    const response = await client.get<JsonOf<UnitData>>(`/views/units/${id}`, {
      params: { from: from.formatIso(), to: to.formatIso() }
    })

    return Success.of({
      ...response.data,
      groups: response.data.groups.map(mapGroupJson),
      placements: response.data.placements.map(mapPlacementJson),
      backupCares: response.data.backupCares.map(mapBackupCareJson),
      missingGroupPlacements: response.data.missingGroupPlacements.map(
        mapMissingGroupPlacementJson
      ),
      unitOccupancies:
        response.data.unitOccupancies &&
        mapUnitOccupancyJson(response.data.unitOccupancies),
      groupOccupancies:
        response.data.groupOccupancies &&
        mapGroupOccupancyJson(response.data.groupOccupancies),
      placementProposals: response.data.placementProposals?.map(
        mapPlacementPlanJson
      ),
      placementPlans: response.data.placementPlans?.map(mapPlacementPlanJson),
      applications: response.data.applications
        ?.map(mapApplicationsJson)
        .sort((applicationA, applicationB) => {
          const lastNameCmp = applicationA.lastName.localeCompare(
            applicationB.lastName,
            'fi',
            { ignorePunctuation: true }
          )
          return lastNameCmp !== 0
            ? lastNameCmp
            : applicationA.firstName.localeCompare(
                applicationB.firstName,
                'fi',
                { ignorePunctuation: true }
              )
        })
    })
  } catch (e) {
    console.error(e)
    return Failure.fromError(e)
  }
}

function mapGroupJson(data: JsonOf<DaycareGroup>): DaycareGroup {
  return {
    ...data,
    startDate: LocalDate.parseIso(data.startDate),
    endDate: LocalDate.parseNullableIso(data.endDate)
  }
}

function mapPlacementJson(data: JsonOf<DaycarePlacement>): DaycarePlacement {
  return {
    ...data,
    child: {
      ...data.child,
      dateOfBirth: LocalDate.parseIso(data.child.dateOfBirth)
    },
    startDate: LocalDate.parseIso(data.startDate),
    endDate: LocalDate.parseIso(data.endDate),
    groupPlacements: data.groupPlacements.map((groupPlacement) => ({
      ...groupPlacement,
      startDate: LocalDate.parseIso(groupPlacement.startDate),
      endDate: LocalDate.parseIso(groupPlacement.endDate),
      type: data.type
    }))
  }
}

function mapBackupCareJson(data: JsonOf<UnitBackupCare>): UnitBackupCare {
  return {
    ...data,
    child: {
      ...data.child,
      birthDate: LocalDate.parseIso(data.child.birthDate)
    },
    period: FiniteDateRange.parseJson(data.period)
  }
}

function mapMissingGroupPlacementJson(
  data: JsonOf<MissingGroupPlacement>
): MissingGroupPlacement {
  return {
    ...data,
    dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
    placementPeriod: FiniteDateRange.parseJson(data.placementPeriod),
    gap: FiniteDateRange.parseJson(data.gap)
  }
}

function mapUnitOccupancyJson(data: JsonOf<UnitOccupancies>): UnitOccupancies {
  return {
    planned: mapOccupancyResponse(data.planned),
    confirmed: mapOccupancyResponse(data.confirmed),
    realized: mapOccupancyResponse(data.realized)
  }
}

function mapGroupOccupancyJson(
  data: JsonOf<GroupOccupancies>
): GroupOccupancies {
  return {
    confirmed: Object.fromEntries(
      Object.entries(data.confirmed).map(([groupId, data]) => [
        groupId,
        mapOccupancyResponse(data)
      ])
    ),
    realized: Object.fromEntries(
      Object.entries(data.realized).map(([groupId, data]) => [
        groupId,
        mapOccupancyResponse(data)
      ])
    )
  }
}

function mapPlacementPlanJson(
  data: JsonOf<DaycarePlacementPlan>
): DaycarePlacementPlan {
  return {
    ...data,
    period: FiniteDateRange.parseJson(data.period),
    preschoolDaycarePeriod: data.preschoolDaycarePeriod
      ? FiniteDateRange.parseJson(data.preschoolDaycarePeriod)
      : null,
    child: {
      ...data.child,
      dateOfBirth: LocalDate.parseIso(data.child.dateOfBirth)
    }
  }
}

function mapApplicationsJson(
  data: JsonOf<ApplicationUnitSummary>
): ApplicationUnitSummary {
  return {
    ...data,
    dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
    preferredStartDate: LocalDate.parseIso(data.preferredStartDate)
  }
}

export async function createPlacement(
  daycarePlacementId: UUID,
  groupId: UUID,
  startDate: LocalDate,
  endDate: LocalDate
): Promise<Result<UUID>> {
  const url = `/placements/${daycarePlacementId}/group-placements`
  const data = {
    groupId,
    startDate: startDate.formatIso(),
    endDate: endDate.formatIso()
  }
  return client
    .post<JsonOf<UUID>>(url, data)
    .then((res) => Success.of(res.data || ''))
    .catch((e) => Failure.fromError(e))
}

export async function transferGroup(
  daycarePlacementId: UUID,
  groupPlacementId: UUID,
  groupId: UUID,
  startDate: LocalDate
): Promise<Result<null>> {
  const url = `/placements/${daycarePlacementId}/group-placements/${groupPlacementId}/transfer`
  const data = {
    groupId,
    startDate: startDate.formatIso()
  }
  return client
    .post(url, data)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function createGroup(
  daycareId: UUID,
  name: string,
  startDate: LocalDate,
  initialCaretakers: number
) {
  const url = `/daycares/${daycareId}/groups`
  const data = {
    name,
    startDate,
    initialCaretakers
  }
  await client.post(url, data)
}

export async function editGroup(
  daycareId: UUID,
  groupId: UUID,
  data: Partial<DaycareGroup>
) {
  const url = `/daycares/${daycareId}/groups/${groupId}`
  await client.put(url, data)
}

export async function deleteGroup(daycareId: UUID, groupId: UUID) {
  const url = `/daycares/${daycareId}/groups/${groupId}`
  await client.delete(url)
}

export async function deletePlacement(
  daycarePlacementId: UUID,
  groupPlacementId: UUID
): Promise<Result<null>> {
  const url = `/placements/${daycarePlacementId}/group-placements/${groupPlacementId}`
  return client
    .delete(url)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export type OccupancyResponse = {
  occupancies: Occupancy[]
  max?: Occupancy
  min?: Occupancy
}

const mapOccupancyPeriod = (p: JsonOf<Occupancy>): Occupancy => ({
  ...p,
  period: FiniteDateRange.parseJson(p.period)
})

const mapOccupancyResponse = (
  r: JsonOf<OccupancyResponse>
): OccupancyResponse => ({
  occupancies: r.occupancies.map(mapOccupancyPeriod),
  min: r.min && mapOccupancyPeriod(r.min),
  max: r.max && mapOccupancyPeriod(r.max)
})

export async function getOccupancyRates(
  unitId: UUID,
  from: LocalDate,
  to: LocalDate,
  type: OccupancyType
): Promise<Result<OccupancyResponse>> {
  return client
    .get<JsonOf<OccupancyResponse>>(`/occupancy/by-unit/${unitId}`, {
      params: {
        from: from.formatIso(),
        to: to.formatIso(),
        type
      }
    })
    .then(({ data }) => mapOccupancyResponse(data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export interface DaycareAclRow {
  employee: DaycareAclRowEmployee
  role: AdRole
  groupIds: UUID[]
}

export interface DaycareAclRowEmployee {
  id: UUID
  firstName: string
  lastName: string
  email: string | null
}

interface DaycareAclResponse {
  rows: DaycareAclRow[]
}

export async function getDaycareAclRows(
  unitId: UUID
): Promise<Result<DaycareAclRow[]>> {
  return client
    .get<JsonOf<DaycareAclResponse>>(`/daycares/${unitId}/acl`)
    .then(({ data }) => Success.of(data.rows))
    .catch((e) => Failure.fromError(e))
}

export async function addDaycareAclSupervisor(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/supervisors/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function removeDaycareAclSupervisor(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/supervisors/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function addDaycareAclSpecialEducationTeacher(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/specialeducationteacher/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function removeDaycareAclSpecialEducationTeacher(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/specialeducationteacher/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function addDaycareAclStaff(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/staff/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function removeDaycareAclStaff(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/staff/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function updateDaycareGroupAcl(
  unitId: UUID,
  employeeId: UUID,
  groupIds: UUID[]
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/staff/${employeeId}/groups`, {
      groupIds
    })
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function deleteMobileDevice(
  mobileId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/mobile-devices/${mobileId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
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

export async function postPairing(
  unitId: UUID
): Promise<Result<PairingResponse>> {
  return client
    .post<JsonOf<PairingResponse>>(`/pairings`, {
      unitId
    })
    .then((res) => res.data)
    .then((pairingResponse) => {
      return {
        ...pairingResponse,
        expires: new Date(pairingResponse.expires)
      }
    })
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postPairingResponse(
  pairingId: UUID,
  challengeKey: string,
  responseKey: string
): Promise<Result<PairingResponse>> {
  return client
    .post<JsonOf<PairingResponse>>(`/pairings/${pairingId}/response`, {
      challengeKey,
      responseKey
    })
    .then((res) => res.data)
    .then((pairingResponse) => {
      return {
        ...pairingResponse,
        expires: new Date(pairingResponse.expires)
      }
    })
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function putMobileDeviceName(
  id: UUID,
  name: string
): Promise<Result<void>> {
  return client
    .put<JsonOf<void>>(`/mobile-devices/${id}/name`, {
      name
    })
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export interface MobileDevice {
  id: UUID
  name: string
}

export async function getMobileDevices(
  unitId: UUID
): Promise<Result<MobileDevice[]>> {
  return client
    .get<JsonOf<MobileDevice[]>>(`/mobile-devices`, {
      params: {
        unitId
      }
    })
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

interface PairingStatusResponse {
  status: PairingStatus
}

export async function getPairingStatus(
  pairingId: UUID
): Promise<Result<PairingStatusResponse>> {
  return client
    .get<JsonOf<PairingStatusResponse>>(`/public/pairings/${pairingId}/status`)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export interface DaycareFields {
  name: string
  openingDate: LocalDate | null
  closingDate: LocalDate | null
  areaId: UUID
  type: UnitTypes[]
  daycareApplyPeriod: DateRange | null
  preschoolApplyPeriod: DateRange | null
  clubApplyPeriod: DateRange | null
  providerType: ProviderType
  roundTheClock: boolean
  capacity: number
  language: UnitLanguage
  ghostUnit: boolean
  uploadToVarda: boolean
  uploadToKoski: boolean
  invoicedByMunicipality: boolean
  costCenter: string | null
  financeDecisionHandlerId: UUID
  additionalInfo: string | null
  phone: string | null
  email: string | null
  url: string | null
  visitingAddress: VisitingAddress
  location: Coordinate | null
  mailingAddress: MailingAddress
  unitManager: UnitManager | null
  decisionCustomization: DecisionCustomization
  ophUnitOid: string | null
  ophOrganizerOid: string | null
  ophOrganizationOid: string | null
  operationDays: DayOfWeek[] | null
}

interface CreateDaycareResponse {
  id: UUID
}

export async function createDaycare(
  fields: DaycareFields
): Promise<Result<UUID>> {
  return client
    .put<JsonOf<CreateDaycareResponse>>('/daycares', fields)
    .then(({ data }) => Success.of(data.id))
    .catch((e) => Failure.fromError(e))
}

export async function updateDaycare(
  id: UUID,
  fields: DaycareFields
): Promise<Result<Unit>> {
  return client
    .put<JsonOf<Unit>>(`/daycares/${encodeURIComponent(id)}`, fields)
    .then(({ data }) => Success.of(convertUnitJson(data)))
    .catch((e) => Failure.fromError(e))
}

export async function getGroupDaycareDailyNotes(
  groupId: UUID
): Promise<Result<DaycareDailyNote[]>> {
  return client
    .get<JsonOf<DaycareDailyNote[]>>(
      `/daycare-daily-note/daycare/group/${groupId}`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        date: LocalDate.parseNullableIso(data.date),
        modifiedAt: data.modifiedAt ? new Date(data.modifiedAt) : null
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function upsertChildDaycareDailyNote(
  childId: string,
  daycareDailyNote: DaycareDailyNoteFormData
): Promise<Result<Unit>> {
  const url = `/daycare-daily-note/child/${childId}`
  return (daycareDailyNote.id
    ? client.put(url, daycareDailyNote)
    : client.post(url, daycareDailyNote)
  )
    .then(({ data }) => Success.of(convertUnitJson(data)))
    .catch((e) => Failure.fromError(e))
}

export async function upsertGroupDaycareDailyNote(
  groupId: string,
  daycareDailyNote: DaycareDailyNoteFormData
): Promise<Result<Unit>> {
  const url = `/daycare-daily-note/group/${groupId}`
  return (daycareDailyNote.id
    ? client.put(url, daycareDailyNote)
    : client.post(url, daycareDailyNote)
  )
    .then(({ data }) => Success.of(convertUnitJson(data)))
    .catch((e) => Failure.fromError(e))
}

export async function deleteDaycareDailyNote(
  noteId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycare-daily-note/${noteId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export interface DaycareDailyNoteFormData {
  id?: UUID
  childId: UUID | null
  groupId: UUID | null
  date: LocalDate | null
  note?: string
  feedingNote?: DaycareDailyNoteLevelInfo
  sleepingNote?: DaycareDailyNoteLevelInfo
  sleepingHours?: string
  reminders: DaycareDailyNoteReminder[]
  reminderNote?: string
  modifiedAt?: Date | null
  modifiedBy?: string
}
