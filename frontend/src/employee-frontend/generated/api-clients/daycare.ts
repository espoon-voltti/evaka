// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AclUpdate } from 'lib-common/generated/api-types/daycare'
import { AdditionalInformation } from 'lib-common/generated/api-types/daycare'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { ApplicationUnitType } from 'lib-common/generated/api-types/daycare'
import { AreaJSON } from 'lib-common/generated/api-types/daycare'
import { CaretakerRequest } from 'lib-common/generated/api-types/daycare'
import { CaretakersResponse } from 'lib-common/generated/api-types/daycare'
import { ChildResponse } from 'lib-common/generated/api-types/daycare'
import { ClubTerm } from 'lib-common/generated/api-types/daycare'
import { ClubTermRequest } from 'lib-common/generated/api-types/daycare'
import { CreateDaycareResponse } from 'lib-common/generated/api-types/daycare'
import { CreateGroupRequest } from 'lib-common/generated/api-types/daycare'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import { DaycareAclRow } from 'lib-common/generated/api-types/shared'
import { DaycareFields } from 'lib-common/generated/api-types/daycare'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import { Employee } from 'lib-common/generated/api-types/pis'
import { FullAclInfo } from 'lib-common/generated/api-types/daycare'
import { GroupUpdateRequest } from 'lib-common/generated/api-types/daycare'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { PreschoolTermRequest } from 'lib-common/generated/api-types/daycare'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { StaffAttendanceForDates } from 'lib-common/generated/api-types/daycare'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import { TemporaryEmployee } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { UnitFeatures } from 'lib-common/generated/api-types/daycare'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import { UnitNotifications } from 'lib-common/generated/api-types/daycare'
import { UnitStub } from 'lib-common/generated/api-types/daycare'
import { UnitTypeFilter } from 'lib-common/generated/api-types/daycare'
import { UpdateFeaturesRequest } from 'lib-common/generated/api-types/daycare'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonCaretakersResponse } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonChildResponse } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonClubTerm } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonDaycare } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonDaycareGroup } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonDaycareResponse } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonPublicUnit } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonStaffAttendanceForDates } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonUnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.daycare.controllers.ChildController.getAdditionalInfo
*/
export async function getAdditionalInfo(
  request: {
    childId: UUID
  }
): Promise<AdditionalInformation> {
  const { data: json } = await client.request<JsonOf<AdditionalInformation>>({
    url: uri`/employee/children/${request.childId}/additional-information`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.ChildController.getChild
*/
export async function getChild(
  request: {
    childId: UUID
  }
): Promise<ChildResponse> {
  const { data: json } = await client.request<JsonOf<ChildResponse>>({
    url: uri`/employee/children/${request.childId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildResponse(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.ChildController.updateAdditionalInfo
*/
export async function updateAdditionalInfo(
  request: {
    childId: UUID,
    body: AdditionalInformation
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/children/${request.childId}/additional-information`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AdditionalInformation>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.createCaretakers
*/
export async function createCaretakers(
  request: {
    daycareId: UUID,
    groupId: UUID,
    body: CaretakerRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}/caretakers`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CaretakerRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.createDaycare
*/
export async function createDaycare(
  request: {
    body: DaycareFields
  }
): Promise<CreateDaycareResponse> {
  const { data: json } = await client.request<JsonOf<CreateDaycareResponse>>({
    url: uri`/employee/daycares`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DaycareFields>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.createGroup
*/
export async function createGroup(
  request: {
    daycareId: UUID,
    body: CreateGroupRequest
  }
): Promise<DaycareGroup> {
  const { data: json } = await client.request<JsonOf<DaycareGroup>>({
    url: uri`/employee/daycares/${request.daycareId}/groups`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateGroupRequest>
  })
  return deserializeJsonDaycareGroup(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.deleteGroup
*/
export async function deleteGroup(
  request: {
    daycareId: UUID,
    groupId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getCaretakers
*/
export async function getCaretakers(
  request: {
    daycareId: UUID,
    groupId: UUID
  }
): Promise<CaretakersResponse> {
  const { data: json } = await client.request<JsonOf<CaretakersResponse>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}/caretakers`.toString(),
    method: 'GET'
  })
  return deserializeJsonCaretakersResponse(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getDaycare
*/
export async function getDaycare(
  request: {
    daycareId: UUID
  }
): Promise<DaycareResponse> {
  const { data: json } = await client.request<JsonOf<DaycareResponse>>({
    url: uri`/employee/daycares/${request.daycareId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonDaycareResponse(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getDaycares
*/
export async function getDaycares(
  request: {
    includeClosed?: boolean | null
  }
): Promise<Daycare[]> {
  const params = createUrlSearchParams(
    ['includeClosed', request.includeClosed?.toString()]
  )
  const { data: json } = await client.request<JsonOf<Daycare[]>>({
    url: uri`/employee/daycares`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDaycare(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getGroups
*/
export async function getGroups(
  request: {
    daycareId: UUID,
    from?: LocalDate | null,
    to?: LocalDate | null
  }
): Promise<DaycareGroup[]> {
  const params = createUrlSearchParams(
    ['from', request.from?.formatIso()],
    ['to', request.to?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DaycareGroup[]>>({
    url: uri`/employee/daycares/${request.daycareId}/groups`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDaycareGroup(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getUnitFeatures
*/
export async function getUnitFeatures(): Promise<UnitFeatures[]> {
  const { data: json } = await client.request<JsonOf<UnitFeatures[]>>({
    url: uri`/employee/daycares/features`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getUnitGroupDetails
*/
export async function getUnitGroupDetails(
  request: {
    unitId: UUID,
    from: LocalDate,
    to: LocalDate
  }
): Promise<UnitGroupDetails> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<UnitGroupDetails>>({
    url: uri`/employee/daycares/${request.unitId}/group-details`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonUnitGroupDetails(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getUnitNotifications
*/
export async function getUnitNotifications(
  request: {
    daycareId: UUID
  }
): Promise<UnitNotifications> {
  const { data: json } = await client.request<JsonOf<UnitNotifications>>({
    url: uri`/employee/daycares/${request.daycareId}/notifications`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.removeCaretakers
*/
export async function removeCaretakers(
  request: {
    daycareId: UUID,
    groupId: UUID,
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}/caretakers/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.updateCaretakers
*/
export async function updateCaretakers(
  request: {
    daycareId: UUID,
    groupId: UUID,
    id: UUID,
    body: CaretakerRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}/caretakers/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<CaretakerRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.updateDaycare
*/
export async function updateDaycare(
  request: {
    daycareId: UUID,
    body: DaycareFields
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DaycareFields>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.updateGroup
*/
export async function updateGroup(
  request: {
    daycareId: UUID,
    groupId: UUID,
    body: GroupUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<GroupUpdateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.updateUnitFeatures
*/
export async function updateUnitFeatures(
  request: {
    body: UpdateFeaturesRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/unit-features`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdateFeaturesRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationController.getAllApplicableUnits
*/
export async function getAllApplicableUnits(
  request: {
    applicationType: ApplicationType
  }
): Promise<PublicUnit[]> {
  const { data: json } = await client.request<JsonOf<PublicUnit[]>>({
    url: uri`/employee/public/units/${request.applicationType}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPublicUnit(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationController.getApplicationUnits
*/
export async function getApplicationUnits(
  request: {
    type: ApplicationUnitType,
    date: LocalDate,
    shiftCare?: boolean | null
  }
): Promise<PublicUnit[]> {
  const params = createUrlSearchParams(
    ['type', request.type.toString()],
    ['date', request.date.formatIso()],
    ['shiftCare', request.shiftCare?.toString()]
  )
  const { data: json } = await client.request<JsonOf<PublicUnit[]>>({
    url: uri`/employee/units`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPublicUnit(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationController.getAreas
*/
export async function getAreas(): Promise<AreaJSON[]> {
  const { data: json } = await client.request<JsonOf<AreaJSON[]>>({
    url: uri`/employee/areas`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.LocationController.getUnits
*/
export async function getUnits(
  request: {
    type: UnitTypeFilter,
    areaIds?: UUID[] | null,
    from?: LocalDate | null
  }
): Promise<UnitStub[]> {
  const params = createUrlSearchParams(
    ['type', request.type.toString()],
    ...(request.areaIds?.map((e): [string, string | null | undefined] => ['areaIds', e]) ?? []),
    ['from', request.from?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<UnitStub[]>>({
    url: uri`/employee/filters/units`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.StaffAttendanceController.getStaffAttendancesByGroup
*/
export async function getStaffAttendancesByGroup(
  request: {
    groupId: UUID,
    year: number,
    month: number
  }
): Promise<StaffAttendanceForDates> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<StaffAttendanceForDates>>({
    url: uri`/employee/staff-attendances/group/${request.groupId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonStaffAttendanceForDates(json)
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.StaffAttendanceController.upsertStaffAttendance
*/
export async function upsertStaffAttendance(
  request: {
    groupId: UUID,
    body: StaffAttendanceUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/staff-attendances/group/${request.groupId}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StaffAttendanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.createClubTerm
*/
export async function createClubTerm(
  request: {
    body: ClubTermRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/club-terms`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ClubTermRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.createPreschoolTerm
*/
export async function createPreschoolTerm(
  request: {
    body: PreschoolTermRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-terms`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PreschoolTermRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.deleteClubTerm
*/
export async function deleteClubTerm(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/club-terms/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.deletePreschoolTerm
*/
export async function deletePreschoolTerm(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-terms/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.getClubTerms
*/
export async function getClubTerms(): Promise<ClubTerm[]> {
  const { data: json } = await client.request<JsonOf<ClubTerm[]>>({
    url: uri`/employee/public/club-terms`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonClubTerm(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.getPreschoolTerms
*/
export async function getPreschoolTerms(): Promise<PreschoolTerm[]> {
  const { data: json } = await client.request<JsonOf<PreschoolTerm[]>>({
    url: uri`/employee/public/preschool-terms`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPreschoolTerm(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.updateClubTerm
*/
export async function updateClubTerm(
  request: {
    id: UUID,
    body: ClubTermRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/club-terms/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ClubTermRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.TermsController.updatePreschoolTerm
*/
export async function updatePreschoolTerm(
  request: {
    id: UUID,
    body: PreschoolTermRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-terms/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PreschoolTermRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.addFullAclForRole
*/
export async function addFullAclForRole(
  request: {
    daycareId: UUID,
    employeeId: UUID,
    body: FullAclInfo
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/full-acl/${request.employeeId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FullAclInfo>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.createTemporaryEmployee
*/
export async function createTemporaryEmployee(
  request: {
    unitId: UUID,
    body: TemporaryEmployee
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/daycares/${request.unitId}/temporary`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<TemporaryEmployee>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteEarlyChildhoodEducationSecretary
*/
export async function deleteEarlyChildhoodEducationSecretary(
  request: {
    daycareId: UUID,
    employeeId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/earlychildhoodeducationsecretary/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteSpecialEducationTeacher
*/
export async function deleteSpecialEducationTeacher(
  request: {
    daycareId: UUID,
    employeeId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/specialeducationteacher/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteStaff
*/
export async function deleteStaff(
  request: {
    daycareId: UUID,
    employeeId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/staff/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteTemporaryEmployee
*/
export async function deleteTemporaryEmployee(
  request: {
    unitId: UUID,
    employeeId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/temporary/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteTemporaryEmployeeAcl
*/
export async function deleteTemporaryEmployeeAcl(
  request: {
    unitId: UUID,
    employeeId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/temporary/${request.employeeId}/acl`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteUnitSupervisor
*/
export async function deleteUnitSupervisor(
  request: {
    daycareId: UUID,
    employeeId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/supervisors/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.getDaycareAcl
*/
export async function getDaycareAcl(
  request: {
    daycareId: UUID
  }
): Promise<DaycareAclRow[]> {
  const { data: json } = await client.request<JsonOf<DaycareAclRow[]>>({
    url: uri`/employee/daycares/${request.daycareId}/acl`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.getTemporaryEmployee
*/
export async function getTemporaryEmployee(
  request: {
    unitId: UUID,
    employeeId: UUID
  }
): Promise<TemporaryEmployee> {
  const { data: json } = await client.request<JsonOf<TemporaryEmployee>>({
    url: uri`/employee/daycares/${request.unitId}/temporary/${request.employeeId}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.getTemporaryEmployees
*/
export async function getTemporaryEmployees(
  request: {
    unitId: UUID
  }
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/daycares/${request.unitId}/temporary`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.updateGroupAclWithOccupancyCoefficient
*/
export async function updateGroupAclWithOccupancyCoefficient(
  request: {
    daycareId: UUID,
    employeeId: UUID,
    body: AclUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/staff/${request.employeeId}/groups`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AclUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.updateTemporaryEmployee
*/
export async function updateTemporaryEmployee(
  request: {
    unitId: UUID,
    employeeId: UUID,
    body: TemporaryEmployee
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/temporary/${request.employeeId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<TemporaryEmployee>
  })
  return json
}
