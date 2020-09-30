// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '~api/index'
import { client } from '~api/client'
import {
  Coordinate,
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
} from '~types/unit'
import { UnitBackupCare } from '~types/child'
import { AdRole, UUID } from '~types'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

function convertUnitJson(unit: JsonOf<Unit>): Unit {
  return {
    ...unit,
    openingDate: unit.openingDate ? LocalDate.parseIso(unit.openingDate) : null,
    closingDate: unit.closingDate ? LocalDate.parseIso(unit.closingDate) : null
  }
}

export async function getDaycares(): Promise<Result<Unit[]>> {
  return client
    .get<JsonOf<Unit[]>>('/daycares')
    .then(({ data }) => Success(data.map(convertUnitJson)))
    .catch(Failure)
}

export interface UnitResponse {
  daycare: Unit
  currentUserRoles: AdRole[]
}

export async function getDaycare(id: UUID): Promise<Result<UnitResponse>> {
  return client
    .get<JsonOf<UnitResponse>>(`/daycares/${id}`)
    .then(({ data }) =>
      Success({ ...data, daycare: convertUnitJson(data.daycare) })
    )
    .catch(Failure)
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

export type UnitData = {
  groups: DaycareGroup[]
  placements: DaycarePlacement[]
  backupCares: UnitBackupCare[]
  caretakers: Caretakers
  unitOccupancies?: UnitOccupancies
  groupOccupancies?: GroupOccupancies
  placementProposals?: DaycarePlacementPlan[]
  placementPlans?: DaycarePlacementPlan[]
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

    return Success({
      ...response.data,
      groups: response.data.groups.map(mapGroupJson),
      placements: response.data.placements.map(mapPlacementJson),
      backupCares: response.data.backupCares.map(mapBackupCareJson),
      unitOccupancies:
        response.data.unitOccupancies &&
        mapUnitOccupancyJson(response.data.unitOccupancies),
      groupOccupancies:
        response.data.groupOccupancies &&
        mapGroupOccupancyJson(response.data.groupOccupancies),
      placementProposals: response.data.placementProposals?.map(
        mapPlacementPlanJson
      ),
      placementPlans: response.data.placementPlans?.map(mapPlacementPlanJson)
    })
  } catch (e) {
    console.error(e)
    return Failure(e)
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
    period: {
      start: LocalDate.parseIso(data.period.start),
      end: LocalDate.parseIso(data.period.end)
    }
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
    period: {
      start: LocalDate.parseIso(data.period.start),
      end: LocalDate.parseIso(data.period.end)
    },
    preschoolDaycarePeriod: data.preschoolDaycarePeriod
      ? {
          start: LocalDate.parseIso(data.preschoolDaycarePeriod.start),
          end: LocalDate.parseIso(data.preschoolDaycarePeriod.end)
        }
      : null,
    child: {
      ...data.child,
      dateOfBirth: LocalDate.parseIso(data.child.dateOfBirth)
    }
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
    .then((res) => Success(res.data || ''))
    .catch(Failure)
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
    .then(() => Success(null))
    .catch(Failure)
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
    .then(() => Success(null))
    .catch(Failure)
}

export type OccupancyResponse = {
  occupancies: Occupancy[]
  max?: Occupancy
  min?: Occupancy
}

const mapOccupancyPeriod = (p: JsonOf<Occupancy>): Occupancy => ({
  ...p,
  period: {
    start: LocalDate.parseIso(p.period.start),
    end: LocalDate.parseIso(p.period.end)
  }
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
    .then(Success)
    .catch(Failure)
}

export interface DaycareAclRow {
  employee: DaycareAclRowEmployee
  role: AdRole
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
    .then(({ data }) => Success(data.rows))
    .catch(Failure)
}

export async function addDaycareAclSupervisor(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/supervisors/${personId}`)
    .then(() => Success(undefined))
    .catch(Failure)
}

export async function removeDaycareAclSupervisor(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/supervisors/${personId}`)
    .then(() => Success(undefined))
    .catch(Failure)
}

export async function addDaycareAclStaff(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .put(`/daycares/${unitId}/staff/${personId}`)
    .then(() => Success(undefined))
    .catch(Failure)
}

export async function removeDaycareAclStaff(
  unitId: UUID,
  personId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daycares/${unitId}/staff/${personId}`)
    .then(() => Success(undefined))
    .catch(Failure)
}

export interface DaycareFields {
  name: string
  openingDate: LocalDate | null
  closingDate: LocalDate | null
  areaId: UUID
  type: UnitTypes[]
  canApplyDaycare: boolean
  canApplyPreschool: boolean
  canApplyClub: boolean
  providerType: ProviderType
  roundTheClock: boolean
  capacity: number
  language: UnitLanguage
  uploadToVarda: boolean
  uploadToKoski: boolean
  invoicedByMunicipality: boolean
  costCenter: string | null
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
}

interface CreateDaycareResponse {
  id: UUID
}

export async function createDaycare(
  fields: DaycareFields
): Promise<Result<UUID>> {
  return client
    .put<JsonOf<CreateDaycareResponse>>('/daycares', fields)
    .then(({ data }) => Success(data.id))
    .catch(Failure)
}

export async function updateDaycare(
  id: UUID,
  fields: DaycareFields
): Promise<Result<Unit>> {
  return client
    .put<JsonOf<Unit>>(`/daycares/${encodeURIComponent(id)}`, fields)
    .then(({ data }) => Success(convertUnitJson(data)))
    .catch(Failure)
}
