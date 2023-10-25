// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareAclRole } from 'employee-frontend/components/unit/tab-unit-information/UnitAccessControl'
import { Failure, Result, Success } from 'lib-common/api'
import {
  parseDailyServiceTimes,
  parseIsoTimeRange
} from 'lib-common/api-types/daily-service-times'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import {
  ApplicationUnitSummary,
  UnitApplications
} from 'lib-common/generated/api-types/application'
import { AttendancesRequest } from 'lib-common/generated/api-types/attendance'
import { UnitBackupCare } from 'lib-common/generated/api-types/backupcare'
import {
  AclUpdate,
  CreateDaycareResponse,
  CreateGroupRequest,
  DaycareAclResponse,
  DaycareFields,
  GroupOccupancies,
  GroupUpdateRequest,
  UnitGroupDetails,
  UnitNotifications
} from 'lib-common/generated/api-types/daycare'
import {
  OccupancyPeriod,
  OccupancyResponse,
  OccupancyResponseSpeculated,
  UnitOccupancies
} from 'lib-common/generated/api-types/occupancy'
import {
  MobileDevice,
  PostPairingReq
} from 'lib-common/generated/api-types/pairing'
import { Employee, TemporaryEmployee } from 'lib-common/generated/api-types/pis'
import {
  DaycarePlacementWithDetails,
  GroupTransferRequestBody,
  MissingGroupPlacement,
  PlacementPlanDetails,
  TerminatedPlacement
} from 'lib-common/generated/api-types/placement'
import {
  DailyReservationRequest,
  UnitAttendanceReservations
} from 'lib-common/generated/api-types/reservations'
import { ServiceNeed } from 'lib-common/generated/api-types/serviceneed'
import { DaycareAclRow } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { parseReservation } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'

import { DaycareGroup, Unit } from '../types/unit'

import { client } from './client'

function convertUnitJson(unit: JsonOf<Unit>): Unit {
  return {
    ...unit,
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
      : null,
    operationTimes: unit.operationTimes
      ? unit.operationTimes.map((item) =>
          item ? parseIsoTimeRange(item) : null
        )
      : []
  }
}

export async function getDaycares(): Promise<Unit[]> {
  return client
    .get<JsonOf<Unit[]>>('/daycares')
    .then(({ data }) => data.map(convertUnitJson))
}

export interface DaycareGroupSummary {
  id: UUID
  name: string
  endDate: LocalDate | null
  permittedActions: Set<Action.Group>
}

export interface UnitResponse {
  daycare: Unit
  groups: DaycareGroupSummary[]
  permittedActions: Set<Action.Unit>
}

export async function getDaycare(id: UUID): Promise<UnitResponse> {
  return client
    .get<JsonOf<UnitResponse>>(`/daycares/${id}`)
    .then(({ data }) => ({
      daycare: convertUnitJson(data.daycare),
      groups: data.groups.map(({ id, name, endDate, permittedActions }) => ({
        id,
        name,
        endDate: LocalDate.parseNullableIso(endDate),
        permittedActions: new Set(permittedActions)
      })),
      permittedActions: new Set(data.permittedActions)
    }))
}

export function getUnitNotifications(id: UUID): Promise<UnitNotifications> {
  return client
    .get<JsonOf<UnitNotifications>>(`/daycares/${id}/notifications`)
    .then((res) => res.data)
}

export function getUnitOccupancies(
  id: UUID,
  from: LocalDate,
  to: LocalDate
): Promise<UnitOccupancies> {
  return client
    .get<JsonOf<UnitOccupancies>>(`/occupancy/units/${id}`, {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((res) => mapUnitOccupancyJson(res.data))
}

export function getUnitApplications(id: UUID): Promise<UnitApplications> {
  return client
    .get<JsonOf<UnitApplications>>(`/v2/applications/units/${id}`)
    .then((res) => mapUnitApplicationsJson(res.data))
}

export function getUnitGroupDetails(
  unitId: UUID,
  from: LocalDate,
  to: LocalDate
): Promise<UnitGroupDetails> {
  return client
    .get<JsonOf<UnitGroupDetails>>(`/daycares/${unitId}/group-details`, {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((response) => ({
      ...response.data,
      groups: response.data.groups.map(mapGroupJson),
      placements: response.data.placements.map(mapPlacementJson),
      backupCares: response.data.backupCares.map(mapBackupCareJson),
      missingGroupPlacements: response.data.missingGroupPlacements.map(
        mapMissingGroupPlacementJson
      ),
      recentlyTerminatedPlacements:
        response.data.recentlyTerminatedPlacements.map(
          mapRecentlyTerminatedPlacementJson
        ),
      groupOccupancies:
        response.data.groupOccupancies &&
        mapGroupOccupancyJson(response.data.groupOccupancies)
    }))
}

function mapGroupJson(data: JsonOf<DaycareGroup>): DaycareGroup {
  return {
    ...data,
    startDate: LocalDate.parseIso(data.startDate),
    endDate: LocalDate.parseNullableIso(data.endDate)
  }
}

function mapPlacementJson(
  data: JsonOf<DaycarePlacementWithDetails>
): DaycarePlacementWithDetails {
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
    ),
    updated: data.updated ? HelsinkiDateTime.parseIso(data.updated) : null
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
    serviceNeeds: data.serviceNeeds.map((json) => ({
      ...json,
      startDate: LocalDate.parseIso(json.startDate),
      endDate: LocalDate.parseIso(json.endDate)
    })),
    gap: FiniteDateRange.parseJson(data.gap)
  }
}

function mapRecentlyTerminatedPlacementJson(
  data: JsonOf<TerminatedPlacement>
): TerminatedPlacement {
  return {
    ...data,
    endDate: LocalDate.parseIso(data.endDate),
    terminationRequestedDate: LocalDate.parseNullableIso(
      data.terminationRequestedDate
    ),
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
      updated: HelsinkiDateTime.parseIso(serviceNeed.option.updated)
    },
    confirmed:
      serviceNeed.confirmed != null
        ? {
            ...serviceNeed.confirmed,
            at:
              serviceNeed.confirmed.at != null
                ? HelsinkiDateTime.parseIso(serviceNeed.confirmed.at)
                : null
          }
        : null,
    updated: HelsinkiDateTime.parseIso(serviceNeed.updated)
  }))
}

const mapOccupancyPeriod = (p: JsonOf<OccupancyPeriod>): OccupancyPeriod => ({
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
              arrived: HelsinkiDateTime.parseIso(attendance.arrived),
              departed: attendance.departed
                ? HelsinkiDateTime.parseIso(attendance.departed)
                : null
            })
          ),
          staffAttendances: data.realtime.staffAttendances.map(
            (attendance) => ({
              ...attendance,
              arrived: HelsinkiDateTime.parseIso(attendance.arrived),
              departed: attendance.departed
                ? HelsinkiDateTime.parseIso(attendance.departed)
                : null
            })
          ),
          childCapacitySumSeries: data.realtime.childCapacitySumSeries.map(
            (dataPoint) => ({
              ...dataPoint,
              time: HelsinkiDateTime.parseIso(dataPoint.time)
            })
          ),
          staffCapacitySumSeries: data.realtime.staffCapacitySumSeries.map(
            (dataPoint) => ({
              ...dataPoint,
              time: HelsinkiDateTime.parseIso(dataPoint.time)
            })
          ),
          occupancySeries: data.realtime.occupancySeries.map((dataPoint) => ({
            ...dataPoint,
            time: HelsinkiDateTime.parseIso(dataPoint.time)
          }))
        }
      : null,
    caretakers: data.caretakers
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

function mapUnitApplicationsJson(
  data: JsonOf<UnitApplications>
): UnitApplications {
  return {
    placementProposals: data.placementProposals.map(mapPlacementPlanJson),
    placementPlans: data.placementPlans.map(mapPlacementPlanJson),
    applications: data.applications
      .map(mapApplicationsJson)
      .sort((applicationA, applicationB) => {
        const lastNameCmp = applicationA.lastName.localeCompare(
          applicationB.lastName,
          'fi',
          { ignorePunctuation: true }
        )
        return lastNameCmp !== 0
          ? lastNameCmp
          : applicationA.firstName.localeCompare(applicationB.firstName, 'fi', {
              ignorePunctuation: true
            })
      })
  }
}

function mapPlacementPlanJson(
  data: JsonOf<PlacementPlanDetails>
): PlacementPlanDetails {
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

export interface CreateGroupPlacement {
  daycarePlacementId: UUID
  groupId: UUID
  startDate: LocalDate
  endDate: LocalDate
}

export async function createGroupPlacement({
  daycarePlacementId,
  groupId,
  startDate,
  endDate
}: CreateGroupPlacement): Promise<void> {
  const url = `/placements/${daycarePlacementId}/group-placements`
  const data = {
    groupId,
    startDate: startDate.formatIso(),
    endDate: endDate.formatIso()
  }
  return client.post<JsonOf<UUID>>(url, data).then(() => undefined)
}

export type TransferGroup = {
  groupPlacementId: UUID
} & GroupTransferRequestBody

export async function transferGroup({
  groupPlacementId,
  groupId,
  startDate
}: TransferGroup): Promise<void> {
  return client
    .post(`/group-placements/${groupPlacementId}/transfer`, {
      groupId,
      startDate: startDate.formatIso()
    })
    .then(() => undefined)
}

export async function deleteGroupPlacement(
  groupPlacementId: UUID
): Promise<void> {
  return client
    .delete(`/group-placements/${groupPlacementId}`)
    .then(() => undefined)
}

export type CreateGroup = { unitId: UUID } & CreateGroupRequest

export async function createGroup({
  unitId,
  name,
  startDate,
  initialCaretakers
}: CreateGroup): Promise<void> {
  return client.post(`/daycares/${unitId}/groups`, {
    name,
    startDate,
    initialCaretakers
  })
}

export async function getDaycareGroups(unitId: UUID): Promise<DaycareGroup[]> {
  return client
    .get<JsonOf<DaycareGroup[]>>(`/daycares/${unitId}/groups`)
    .then(({ data }) =>
      data.map((group) => ({
        ...group,
        startDate: LocalDate.parseIso(group.startDate),
        endDate: LocalDate.parseNullableIso(group.endDate)
      }))
    )
}

export interface UpdateGroup {
  unitId: UUID
  groupId: UUID
  body: GroupUpdateRequest
}

export async function updateGroup({
  unitId,
  groupId,
  body
}: UpdateGroup): Promise<void> {
  return client
    .put(`/daycares/${unitId}/groups/${groupId}`, body)
    .then(() => undefined)
}

export interface DeleteGroup {
  unitId: UUID
  groupId: UUID
}

export async function deleteGroup({
  unitId,
  groupId
}: DeleteGroup): Promise<void> {
  return client
    .delete(`/daycares/${unitId}/groups/${groupId}`)
    .then(() => undefined)
}

export async function getSpeculatedOccupancyRates(
  applicationId: UUID,
  unitId: UUID,
  from: LocalDate,
  to: LocalDate,
  preschoolDaycareFrom?: LocalDate,
  preschoolDaycareTo?: LocalDate
): Promise<OccupancyResponseSpeculated> {
  return client
    .get<JsonOf<OccupancyResponseSpeculated>>(
      `/occupancy/by-unit/${unitId}/speculated/${applicationId}`,
      {
        params: {
          from: from.formatIso(),
          to: to.formatIso(),
          preschoolDaycareFrom: preschoolDaycareFrom?.formatIso(),
          preschoolDaycareTo: preschoolDaycareTo?.formatIso()
        }
      }
    )
    .then((res) => res.data)
}

export async function getDaycareAclRows(
  unitId: UUID
): Promise<Result<DaycareAclRow[]>> {
  return client
    .get<JsonOf<DaycareAclResponse>>(`/daycares/${unitId}/acl`)
    .then(({ data }) => Success.of(data.aclRows))
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

export async function removeDaycareAclSpecialEducationTeacher(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/specialeducationteacher/${personId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function removeDaycareAclEarlyChildhoodEducationSecretary(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/earlychildhoodeducationsecretary/${personId}`)
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
  update: AclUpdate
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/staff/${employeeId}/groups`, {
      ...update
    })
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function addDaycareFullAcl(
  unitId: UUID,
  employeeId: UUID,
  role: DaycareAclRole,
  update: AclUpdate
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/full-acl/${employeeId}`, {
      update,
      role
    })
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export function getTemporaryEmployees(
  unitId: UUID
): Promise<Result<Employee[]>> {
  return client
    .get<JsonOf<Employee[]>>(`/daycares/${unitId}/temporary`)
    .then(({ data }) =>
      Success.of(
        data.map((item) => ({
          ...item,
          created: HelsinkiDateTime.parseIso(item.created),
          updated:
            item.updated !== null
              ? HelsinkiDateTime.parseIso(item.updated)
              : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export function createTemporaryEmployee(
  unitId: UUID,
  data: TemporaryEmployee
): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/daycares/${unitId}/temporary`, data)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function getTemporaryEmployee(
  unitId: UUID,
  employeeId: UUID
): Promise<Result<TemporaryEmployee>> {
  return client
    .get<JsonOf<TemporaryEmployee>>(
      `/daycares/${unitId}/temporary/${employeeId}`
    )
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function updateTemporaryEmployee(
  unitId: UUID,
  employeeId: UUID,
  data: TemporaryEmployee
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/temporary/${employeeId}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteTemporaryEmployeeAcl(
  unitId: UUID,
  employeeId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/temporary/${employeeId}/acl`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteTemporaryEmployee(
  unitId: UUID,
  employeeId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/temporary/${employeeId}`)
    .then(() => Success.of())
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
  expires: HelsinkiDateTime
  status: PairingStatus
  mobileDeviceId: UUID | null
}

export async function postPairing(
  data: PostPairingReq
): Promise<Result<PairingResponse>> {
  return client
    .post<JsonOf<PairingResponse>>(`/pairings`, data)
    .then((res) => res.data)
    .then((pairingResponse) => ({
      ...pairingResponse,
      expires: HelsinkiDateTime.parseIso(pairingResponse.expires)
    }))
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
    .then((pairingResponse) => ({
      ...pairingResponse,
      expires: HelsinkiDateTime.parseIso(pairingResponse.expires)
    }))
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

export async function createDaycare(fields: DaycareFields): Promise<UUID> {
  return client
    .post<JsonOf<CreateDaycareResponse>>('/daycares', fields)
    .then(({ data }) => data.id)
}

export async function updateDaycare({
  id,
  fields
}: {
  id: UUID
  fields: DaycareFields
}): Promise<void> {
  return client
    .put<JsonOf<Unit>>(`/daycares/${encodeURIComponent(id)}`, fields)
    .then(() => undefined)
}

export async function postReservations(
  reservations: DailyReservationRequest[]
): Promise<void> {
  return client
    .post('/attendance-reservations', reservations)
    .then(() => undefined)
}

export async function postAttendances({
  childId,
  unitId,
  attendances
}: {
  childId: UUID
  unitId: UUID
  attendances: AttendancesRequest
}): Promise<void> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}`, attendances)
    .then(() => undefined)
}

export async function getUnitAttendanceReservations(
  unitId: UUID,
  dateRange: FiniteDateRange,
  includeNonOperationalDays: boolean
): Promise<UnitAttendanceReservations> {
  return client
    .get<JsonOf<UnitAttendanceReservations>>('/attendance-reservations', {
      params: {
        unitId,
        from: dateRange.start.formatIso(),
        to: dateRange.end?.formatIso(),
        includeNonOperationalDays
      }
    })
    .then(({ data }) => ({
      ...data,
      children: data.children.map((child) => ({
        ...child,
        dateOfBirth: LocalDate.parseIso(child.dateOfBirth),
        serviceNeeds: child.serviceNeeds.map((sn) => ({
          ...sn,
          validDuring: FiniteDateRange.parseJson(sn.validDuring)
        }))
      })),
      days: data.days.map((day) => ({
        ...day,
        children: day.children.map((child) => ({
          ...child,
          reservations: child.reservations.map(parseReservation),
          attendances: child.attendances.map(({ startTime, endTime }) => ({
            startTime: LocalTime.parseIso(startTime),
            endTime: endTime ? LocalTime.parseIso(endTime) : null
          })),
          dailyServiceTimes: child.dailyServiceTimes
            ? parseDailyServiceTimes(child.dailyServiceTimes)
            : null
        })),
        date: LocalDate.parseIso(day.date),
        dateInfo: {
          ...day.dateInfo,
          time:
            day.dateInfo.time !== null
              ? {
                  start: LocalTime.parseIso(day.dateInfo.time.start),
                  end: LocalTime.parseIso(day.dateInfo.time.end)
                }
              : null
        }
      }))
    }))
}
