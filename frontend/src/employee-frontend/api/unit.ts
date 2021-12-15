// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import { client } from './client'
import {
  Coordinate,
  DaycareGroup,
  DaycarePlacement,
  DaycarePlacementPlan,
  DecisionCustomization,
  MailingAddress,
  Occupancy,
  OccupancyType,
  Stats, TerminatedPlacement,
  Unit,
  UnitLanguage,
  UnitManager,
  UnitTypes,
  VisitingAddress
} from '../types/unit'
import { DayOfWeek } from '../types'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'
import DateRange from 'lib-common/date-range'
import {
  ServiceNeed,
  ServiceNeedOptionSummary
} from 'lib-common/generated/api-types/serviceneed'
import { UnitProviderType } from 'lib-customizations/types'
import {
  AbsenceType,
  ApplicationStatus,
  PlacementType
} from 'lib-common/generated/enums'
import { Action } from 'lib-common/generated/action'
import { mapValues } from 'lodash'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import {
  ChildReservations,
  UnitAttendanceReservations
} from 'lib-common/api-types/reservations'
import { AdRole } from 'lib-common/api-types/employee-auth'
import { UUID } from 'lib-common/types'
import { MobileDevice } from 'lib-common/generated/api-types/pairing'

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
  permittedActions: Set<Action.Group>
}

export interface UnitResponse {
  daycare: Unit
  groups: DaycareGroupSummary[]
  permittedActions: Set<Action.Unit>
}

export async function getDaycare(id: UUID): Promise<Result<UnitResponse>> {
  return client
    .get<JsonOf<UnitResponse>>(`/daycares/${id}`)
    .then(({ data }) =>
      Success.of({
        daycare: convertUnitJson(data.daycare),
        groups: data.groups.map(({ id, name, permittedActions }) => ({
          id,
          name,
          permittedActions: new Set(permittedActions)
        })),
        permittedActions: new Set(data.permittedActions)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export type UnitOccupancies = {
  planned: OccupancyResponse
  confirmed: OccupancyResponse
  realized: OccupancyResponse
  realtime: RealtimeOccupancy | null
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
  serviceNeeds: ServiceNeed[]
}

interface MissingGroupPlacementBackupCare extends MissingGroupPlacementCommon {
  placementType: null
  backup: true
  serviceNeeds: ServiceNeed[]
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
  serviceNeed: ServiceNeedOptionSummary | null
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
  recentlyTerminatedPlacements: TerminatedPlacement[]
  placementProposals?: DaycarePlacementPlan[]
  placementPlans?: DaycarePlacementPlan[]
  applications?: ApplicationUnitSummary[]
  permittedPlacementActions: Record<UUID, Set<Action.Placement>>
  permittedBackupCareActions: Record<UUID, Set<Action.BackupCare>>
  permittedGroupPlacementActions: Record<UUID, Set<Action.GroupPlacement>>
}

export async function getUnitData(
  id: UUID,
  from: LocalDate,
  to: LocalDate
): Promise<Result<UnitData>> {
  try {
    const response = await client.get<JsonOf<UnitData>>(`/views/units/${id}`, {
      params: {
        from: from.formatIso(),
        to: to.formatIso()
      }
    })

    return Success.of({
      ...response.data,
      groups: response.data.groups.map(mapGroupJson),
      placements: response.data.placements.map(mapPlacementJson),
      backupCares: response.data.backupCares.map(mapBackupCareJson),
      missingGroupPlacements: response.data.missingGroupPlacements.map(
        mapMissingGroupPlacementJson
      ),
      recentlyTerminatedPlacements: response.data.recentlyTerminatedPlacements.map(
        mapRecentlyTerminatedPlacementJson
      ),
      unitOccupancies:
        response.data.unitOccupancies &&
        mapUnitOccupancyJson(response.data.unitOccupancies),
      groupOccupancies:
        response.data.groupOccupancies &&
        mapGroupOccupancyJson(response.data.groupOccupancies),
      placementProposals:
        response.data.placementProposals?.map(mapPlacementPlanJson),
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
        }),
      permittedPlacementActions: mapValues(
        response.data.permittedPlacementActions,
        (actions) => new Set(actions)
      ),
      permittedBackupCareActions: mapValues(
        response.data.permittedBackupCareActions,
        (actions) => new Set(actions)
      ),
      permittedGroupPlacementActions: mapValues(
        response.data.permittedGroupPlacementActions,
        (actions) => new Set(actions)
      )
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
    serviceNeeds: mapServiceNeedsJson(data.serviceNeeds),
    groupPlacements: data.groupPlacements.map((groupPlacement) => ({
      ...groupPlacement,
      startDate: LocalDate.parseIso(groupPlacement.startDate),
      endDate: LocalDate.parseIso(groupPlacement.endDate),
      type: data.type
    })),
    terminationRequestedDate: LocalDate.parseNullableIso(
      data.terminationRequestedDate
    )
  }
}

function mapBackupCareJson(data: JsonOf<UnitBackupCare>): UnitBackupCare {
  return {
    ...data,
    child: {
      ...data.child,
      birthDate: LocalDate.parseIso(data.child.birthDate)
    },
    serviceNeeds: mapServiceNeedsJson(data.serviceNeeds),
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
    serviceNeeds: mapServiceNeedsJson(data.serviceNeeds),
    gap: FiniteDateRange.parseJson(data.gap)
  }
}

function mapRecentlyTerminatedPlacementJson(
  data: JsonOf<TerminatedPlacement>
): TerminatedPlacement {
  return {
    ...data,
    endDate: LocalDate.parseIso(data.endDate),
    terminationRequestedDate: LocalDate.parseIso(data.terminationRequestedDate),
    child: {
      ...data.child,
      dateOfBirth: LocalDate.parseIso(data.child.dateOfBirth)
    }
  }
}

function mapServiceNeedsJson(data: JsonOf<ServiceNeed[]>): ServiceNeed[] {
  return data.map((serviceNeed) => ({
    ...serviceNeed,
    startDate: LocalDate.parseIso(serviceNeed.startDate),
    endDate: LocalDate.parseIso(serviceNeed.endDate),
    option: {
      ...serviceNeed.option,
      updated: new Date(serviceNeed.option.updated)
    },
    confirmed:
      serviceNeed.confirmed != null
        ? {
            ...serviceNeed.confirmed,
            at:
              serviceNeed.confirmed.at != null
                ? new Date(serviceNeed.confirmed.at)
                : null
          }
        : null,
    updated: new Date(serviceNeed.updated)
  }))
}

function mapUnitOccupancyJson(data: JsonOf<UnitOccupancies>): UnitOccupancies {
  return {
    planned: mapOccupancyResponse(data.planned),
    confirmed: mapOccupancyResponse(data.confirmed),
    realized: mapOccupancyResponse(data.realized),
    realtime: data.realtime
      ? {
          childAttendances: data.realtime.childAttendances.map(
            (attendance) => ({
              ...attendance,
              arrived: new Date(attendance.arrived),
              departed: attendance.departed
                ? new Date(attendance.departed)
                : null
            })
          ),
          staffAttendances: data.realtime.staffAttendances.map(
            (attendance) => ({
              ...attendance,
              arrived: new Date(attendance.arrived),
              departed: attendance.departed
                ? new Date(attendance.departed)
                : null
            })
          ),
          childCapacitySumSeries: data.realtime.childCapacitySumSeries.map(
            (dataPoint) => ({
              ...dataPoint,
              time: new Date(dataPoint.time)
            })
          ),
          staffCountSeries: data.realtime.staffCountSeries.map((dataPoint) => ({
            ...dataPoint,
            time: new Date(dataPoint.time)
          })),
          occupancySeries: data.realtime.occupancySeries.map((dataPoint) => ({
            ...dataPoint,
            time: new Date(dataPoint.time)
          }))
        }
      : null
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
    serviceNeed: data.serviceNeed
      ? {
          ...data.serviceNeed,
          updated: new Date(data.serviceNeed.updated)
        }
      : null,
    dateOfBirth: LocalDate.parseIso(data.dateOfBirth),
    preferredStartDate: LocalDate.parseIso(data.preferredStartDate)
  }
}

export async function createGroupPlacement(
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
  groupPlacementId: UUID,
  groupId: UUID,
  startDate: LocalDate
): Promise<Result<null>> {
  const url = `/group-placements/${groupPlacementId}/transfer`
  const data = {
    groupId,
    startDate: startDate.formatIso()
  }
  return client
    .post(url, data)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteGroupPlacement(
  groupPlacementId: UUID
): Promise<Result<null>> {
  const url = `/group-placements/${groupPlacementId}`
  return client
    .delete(url)
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

export async function getDaycareGroups(
  unitId: UUID
): Promise<Result<DaycareGroup[]>> {
  return client
    .get<JsonOf<DaycareGroup[]>>(`/daycares/${unitId}/groups`)
    .then(({ data }) =>
      Success.of(
        data.map((group) => ({
          ...group,
          startDate: LocalDate.parseIso(group.startDate),
          endDate: LocalDate.parseNullableIso(group.endDate)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
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
  data: { unitId: UUID } | { employeeId: UUID }
): Promise<Result<PairingResponse>> {
  return client
    .post<JsonOf<PairingResponse>>(`/pairings`, data)
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
  providerType: UnitProviderType
  roundTheClock: boolean
  capacity: number
  language: UnitLanguage
  ghostUnit: boolean
  uploadToVarda: boolean
  uploadChildrenToVarda: boolean
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
  operationDays: DayOfWeek[] | null
}

interface CreateDaycareResponse {
  id: UUID
}

export async function createDaycare(
  fields: DaycareFields
): Promise<Result<UUID>> {
  return client
    .post<JsonOf<CreateDaycareResponse>>('/daycares', fields)
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

export async function postReservations(
  reservations: DailyReservationRequest[]
): Promise<Result<void>> {
  return client
    .post('/attendance-reservations', reservations)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function getUnitAttendanceReservations(
  unitId: UUID,
  dateRange: FiniteDateRange
): Promise<Result<UnitAttendanceReservations>> {
  return client
    .get<JsonOf<UnitAttendanceReservations>>('/attendance-reservations', {
      params: {
        unitId,
        from: dateRange.start.formatIso(),
        to: dateRange.end?.formatIso()
      }
    })
    .then(({ data }) =>
      Success.of({
        unit: data.unit,
        operationalDays: data.operationalDays.map(({ date, isHoliday }) => ({
          date: LocalDate.parseIso(date),
          isHoliday
        })),
        groups: data.groups.map(({ group, children }) => ({
          group,
          children: children.map(mapChildReservationJson)
        })),
        ungrouped: data.ungrouped.map(mapChildReservationJson)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export interface DailyChildData {
  reservation: { startTime: string; endTime: string } | null
  attendance: { startTime: string; endTime: string | null } | null
  absence: { type: AbsenceType } | null
}

const mapChildReservationJson = (
  json: JsonOf<ChildReservations>
): ChildReservations => ({
  ...json,
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  }
})
