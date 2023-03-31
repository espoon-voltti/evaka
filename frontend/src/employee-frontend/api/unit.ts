// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import { DaycareAclRole } from 'employee-frontend/components/unit/tab-unit-information/UnitAccessControl'
import { Failure, Result, Success } from 'lib-common/api'
import { parseDailyServiceTimes } from 'lib-common/api-types/daily-service-times'
import { AdRole } from 'lib-common/api-types/employee-auth'
import {
  ChildDailyRecords,
  UnitAttendanceReservations,
  UnitServiceNeedInfo
} from 'lib-common/api-types/reservations'
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
  CreateDaycareResponse,
  DaycareFields,
  GroupOccupancies,
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
import {
  DaycarePlacementWithDetails,
  MissingGroupPlacement,
  PlacementPlanDetails,
  TerminatedPlacement
} from 'lib-common/generated/api-types/placement'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import { ServiceNeed } from 'lib-common/generated/api-types/serviceneed'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
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
  endDate: LocalDate | null
  permittedActions: Set<Action.Group>
}

export interface UnitResponse {
  daycare: Unit
  groups: DaycareGroupSummary[]
  permittedActions: Set<Action.Unit>
}

export interface AclUpdateDetails {
  groupIds?: UUID[]
  occupancyCoefficient?: number
}

export async function getDaycare(id: UUID): Promise<Result<UnitResponse>> {
  return client
    .get<JsonOf<UnitResponse>>(`/daycares/${id}`)
    .then(({ data }) =>
      Success.of({
        daycare: convertUnitJson(data.daycare),
        groups: data.groups.map(({ id, name, endDate, permittedActions }) => ({
          id,
          name,
          endDate: LocalDate.parseNullableIso(endDate),
          permittedActions: new Set(permittedActions)
        })),
        permittedActions: new Set(data.permittedActions)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export function getUnitNotifications(
  id: UUID
): Promise<Result<UnitNotifications>> {
  return client
    .get<JsonOf<UnitNotifications>>(`/daycares/${id}/notifications`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export function getUnitOccupancies(
  id: UUID,
  from: LocalDate,
  to: LocalDate
): Promise<Result<UnitOccupancies>> {
  return client
    .get<JsonOf<UnitOccupancies>>(`/occupancy/units/${id}`, {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((res) => Success.of(mapUnitOccupancyJson(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function getUnitApplications(
  id: UUID
): Promise<Result<UnitApplications>> {
  return client
    .get<JsonOf<UnitApplications>>(`/v2/applications/units/${id}`)
    .then((res) => Success.of(mapUnitApplicationsJson(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function getUnitGroupDetails(
  id: UUID,
  from: LocalDate,
  to: LocalDate
): Promise<Result<UnitGroupDetails>> {
  return client
    .get<JsonOf<UnitGroupDetails>>(`/daycares/${id}/group-details`, {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((response) =>
      Success.of({
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
      })
    )
    .catch((e) => Failure.fromError(e))
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

export async function getSpeculatedOccupancyRates(
  applicationId: UUID,
  unitId: UUID,
  from: LocalDate,
  to: LocalDate,
  preschoolDaycareFrom?: LocalDate,
  preschoolDaycareTo?: LocalDate
): Promise<Result<OccupancyResponseSpeculated>> {
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
    .then((res) => Success.of(res.data))
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
  update: AclUpdateDetails
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
  update: AclUpdateDetails
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/full-acl/${employeeId}`, {
      update,
      role
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
    .then((pairingResponse) => {
      return {
        ...pairingResponse,
        expires: HelsinkiDateTime.parseIso(pairingResponse.expires)
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
        expires: HelsinkiDateTime.parseIso(pairingResponse.expires)
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

export async function postAttendances(
  childId: UUID,
  unitId: UUID,
  attendances: AttendancesRequest
): Promise<Result<void>> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}`, attendances)
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
          children: children.map(toChildDayRows)
        })),
        ungrouped: data.ungrouped.map(toChildDayRows),
        unitServiceNeedInfo: deserializeUnitServiceNeedInfo(
          data.unitServiceNeedInfo
        )
      })
    )
    .catch((e) => Failure.fromError(e))
}

const toChildDayRows = (
  json: JsonOf<ChildDailyRecords>
): ChildDailyRecords => ({
  ...json,
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  },
  dailyData: json.dailyData.map((record) =>
    mapValues(record, (daily) => ({
      ...daily,
      reservation: daily.reservation
        ? parseReservation(daily.reservation)
        : daily.reservation,
      dailyServiceTimes: daily.dailyServiceTimes
        ? parseDailyServiceTimes(daily.dailyServiceTimes)
        : null
    }))
  )
})

const deserializeUnitServiceNeedInfo = (
  info: JsonOf<UnitServiceNeedInfo>
): UnitServiceNeedInfo => {
  return {
    ...info,
    groups: info.groups.map((group) => ({
      ...group,
      childInfos: group.childInfos.map((cinfo) => ({
        ...cinfo,
        validDuring: FiniteDateRange.parseJson(cinfo.validDuring)
      }))
    })),
    ungrouped: info.ungrouped.map((uci) => ({
      ...uci,
      validDuring: FiniteDateRange.parseJson(uci.validDuring)
    }))
  }
}
