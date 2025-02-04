// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AclUpdate } from 'lib-common/generated/api-types/daycare'
import { AdditionalInformation } from 'lib-common/generated/api-types/daycare'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { ApplicationUnitType } from 'lib-common/generated/api-types/daycare'
import { AreaId } from 'lib-common/generated/api-types/shared'
import { AreaJSON } from 'lib-common/generated/api-types/daycare'
import { CaretakerRequest } from 'lib-common/generated/api-types/daycare'
import { CaretakersResponse } from 'lib-common/generated/api-types/daycare'
import { ChildResponse } from 'lib-common/generated/api-types/daycare'
import { ClubTerm } from 'lib-common/generated/api-types/daycare'
import { ClubTermId } from 'lib-common/generated/api-types/shared'
import { ClubTermRequest } from 'lib-common/generated/api-types/daycare'
import { CreateDaycareResponse } from 'lib-common/generated/api-types/daycare'
import { CreateGroupRequest } from 'lib-common/generated/api-types/daycare'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import { DaycareAclRow } from 'lib-common/generated/api-types/shared'
import { DaycareCaretakerId } from 'lib-common/generated/api-types/shared'
import { DaycareFields } from 'lib-common/generated/api-types/daycare'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { DaycareId } from 'lib-common/generated/api-types/shared'
import { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import { Employee } from 'lib-common/generated/api-types/pis'
import { EmployeeId } from 'lib-common/generated/api-types/shared'
import { FullAclInfo } from 'lib-common/generated/api-types/daycare'
import { GroupId } from 'lib-common/generated/api-types/shared'
import { GroupUpdateRequest } from 'lib-common/generated/api-types/daycare'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { PreschoolTermId } from 'lib-common/generated/api-types/shared'
import { PreschoolTermRequest } from 'lib-common/generated/api-types/daycare'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { ScheduledDaycareAclRow } from 'lib-common/generated/api-types/shared'
import { ServiceWorkerNote } from 'lib-common/generated/api-types/daycare'
import { StaffAttendanceForDates } from 'lib-common/generated/api-types/daycare'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import { TemporaryEmployee } from 'lib-common/generated/api-types/pis'
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
import { deserializeJsonDaycareAclRow } from 'lib-common/generated/api-types/shared'
import { deserializeJsonDaycareGroup } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonDaycareResponse } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonPublicUnit } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonScheduledDaycareAclRow } from 'lib-common/generated/api-types/shared'
import { deserializeJsonStaffAttendanceForDates } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonUnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.daycare.controllers.ChildController.getAdditionalInfo
*/
export async function getAdditionalInfo(
  request: {
    childId: PersonId
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
    childId: PersonId
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
    childId: PersonId,
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
    daycareId: DaycareId,
    groupId: GroupId,
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
    daycareId: DaycareId,
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
    daycareId: DaycareId,
    groupId: GroupId
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
    daycareId: DaycareId,
    groupId: GroupId
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
    daycareId: DaycareId
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
    daycareId: DaycareId,
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
    unitId: DaycareId,
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
    daycareId: DaycareId
  }
): Promise<UnitNotifications> {
  const { data: json } = await client.request<JsonOf<UnitNotifications>>({
    url: uri`/employee/daycares/${request.daycareId}/notifications`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.getUnitServiceWorkerNote
*/
export async function getUnitServiceWorkerNote(
  request: {
    daycareId: DaycareId
  }
): Promise<ServiceWorkerNote> {
  const { data: json } = await client.request<JsonOf<ServiceWorkerNote>>({
    url: uri`/employee/daycares/${request.daycareId}/service-worker-note`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.removeCaretakers
*/
export async function removeCaretakers(
  request: {
    daycareId: DaycareId,
    groupId: GroupId,
    id: DaycareCaretakerId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/groups/${request.groupId}/caretakers/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.setUnitServiceWorkerNote
*/
export async function setUnitServiceWorkerNote(
  request: {
    daycareId: DaycareId,
    body: ServiceWorkerNote
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.daycareId}/service-worker-note`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ServiceWorkerNote>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.updateCaretakers
*/
export async function updateCaretakers(
  request: {
    daycareId: DaycareId,
    groupId: GroupId,
    id: DaycareCaretakerId,
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
    daycareId: DaycareId,
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
    daycareId: DaycareId,
    groupId: GroupId,
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
* Generated from fi.espoo.evaka.daycare.controllers.DaycareController.updateUnitClosingDate
*/
export async function updateUnitClosingDate(
  request: {
    unitId: DaycareId,
    closingDate: LocalDate
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['closingDate', request.closingDate.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/closing-date`.toString(),
    method: 'PUT',
    params
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
    areaIds?: AreaId[] | null,
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
    groupId: GroupId,
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
    groupId: GroupId,
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
    id: ClubTermId
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
    id: PreschoolTermId
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
    id: ClubTermId,
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
    id: PreschoolTermId,
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
    unitId: DaycareId,
    employeeId: EmployeeId,
    body: FullAclInfo
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/full-acl/${request.employeeId}`.toString(),
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
    unitId: DaycareId,
    body: TemporaryEmployee
  }
): Promise<EmployeeId> {
  const { data: json } = await client.request<JsonOf<EmployeeId>>({
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
    unitId: DaycareId,
    employeeId: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/earlychildhoodeducationsecretary/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteSpecialEducationTeacher
*/
export async function deleteSpecialEducationTeacher(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/specialeducationteacher/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteStaff
*/
export async function deleteStaff(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/staff/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.deleteTemporaryEmployee
*/
export async function deleteTemporaryEmployee(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId
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
    unitId: DaycareId,
    employeeId: EmployeeId
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
    unitId: DaycareId,
    employeeId: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/supervisors/${request.employeeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.getDaycareAcl
*/
export async function getDaycareAcl(
  request: {
    unitId: DaycareId
  }
): Promise<DaycareAclRow[]> {
  const { data: json } = await client.request<JsonOf<DaycareAclRow[]>>({
    url: uri`/employee/daycares/${request.unitId}/acl`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonDaycareAclRow(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.getScheduledDaycareAcl
*/
export async function getScheduledDaycareAcl(
  request: {
    unitId: DaycareId
  }
): Promise<ScheduledDaycareAclRow[]> {
  const { data: json } = await client.request<JsonOf<ScheduledDaycareAclRow[]>>({
    url: uri`/employee/daycares/${request.unitId}/scheduled-acl`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonScheduledDaycareAclRow(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.getTemporaryEmployee
*/
export async function getTemporaryEmployee(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId
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
    unitId: DaycareId
  }
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/daycares/${request.unitId}/temporary`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.reactivateTemporaryEmployee
*/
export async function reactivateTemporaryEmployee(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/temporary/${request.employeeId}/reactivate`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.daycare.controllers.UnitAclController.updateGroupAclWithOccupancyCoefficient
*/
export async function updateGroupAclWithOccupancyCoefficient(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId,
    body: AclUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycares/${request.unitId}/staff/${request.employeeId}/groups`.toString(),
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
    unitId: DaycareId,
    employeeId: EmployeeId,
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
