// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from 'lib-common/date-range'
import { AddSsnRequest } from 'lib-common/generated/api-types/pis'
import { CreateFosterParentRelationshipBody } from 'lib-common/generated/api-types/pis'
import { CreatePersonBody } from 'lib-common/generated/api-types/pis'
import { CreateSsnEmployeeResponse } from 'lib-common/generated/api-types/pis'
import { DaycareId } from 'lib-common/generated/api-types/shared'
import { DisableSsnRequest } from 'lib-common/generated/api-types/pis'
import { Employee } from 'lib-common/generated/api-types/pis'
import { EmployeeId } from 'lib-common/generated/api-types/shared'
import { EmployeePreferredFirstName } from 'lib-common/generated/api-types/pis'
import { EmployeeSetPreferredFirstNameUpdateRequest } from 'lib-common/generated/api-types/pis'
import { EmployeeWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { EvakaRightsRequest } from 'lib-common/generated/api-types/pis'
import { FamilyContact } from 'lib-common/generated/api-types/pis'
import { FamilyContactPriorityUpdate } from 'lib-common/generated/api-types/pis'
import { FamilyContactUpdate } from 'lib-common/generated/api-types/pis'
import { FamilyOverview } from 'lib-common/generated/api-types/pis'
import { FosterParentId } from 'lib-common/generated/api-types/shared'
import { FosterParentRelationship } from 'lib-common/generated/api-types/pis'
import { GetOrCreatePersonBySsnRequest } from 'lib-common/generated/api-types/pis'
import { GuardiansResponse } from 'lib-common/generated/api-types/pis'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MergeRequest } from 'lib-common/generated/api-types/pis'
import { NewEmployee } from 'lib-common/generated/api-types/pis'
import { NewSsnEmployee } from 'lib-common/generated/api-types/pis'
import { PagedEmployeesWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { Parentship } from 'lib-common/generated/api-types/pis'
import { ParentshipId } from 'lib-common/generated/api-types/shared'
import { ParentshipRequest } from 'lib-common/generated/api-types/pis'
import { ParentshipUpdateRequest } from 'lib-common/generated/api-types/pis'
import { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import { Partnership } from 'lib-common/generated/api-types/pis'
import { PartnershipId } from 'lib-common/generated/api-types/shared'
import { PartnershipRequest } from 'lib-common/generated/api-types/pis'
import { PartnershipUpdateRequest } from 'lib-common/generated/api-types/pis'
import { PartnershipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { PersonIdentityResponseJSON } from 'lib-common/generated/api-types/pis'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { PersonPatch } from 'lib-common/generated/api-types/pis'
import { PersonResponse } from 'lib-common/generated/api-types/pis'
import { PersonSummary } from 'lib-common/generated/api-types/pis'
import { PersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { PinCode } from 'lib-common/generated/api-types/pis'
import { SearchEmployeeRequest } from 'lib-common/generated/api-types/pis'
import { SearchPersonBody } from 'lib-common/generated/api-types/pis'
import { UpsertEmployeeDaycareRolesRequest } from 'lib-common/generated/api-types/pis'
import { Uri } from 'lib-common/uri'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { deserializeJsonEmployeeWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { deserializeJsonFamilyOverview } from 'lib-common/generated/api-types/pis'
import { deserializeJsonFosterParentRelationship } from 'lib-common/generated/api-types/pis'
import { deserializeJsonGuardiansResponse } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPagedEmployeesWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { deserializeJsonParentship } from 'lib-common/generated/api-types/pis'
import { deserializeJsonParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPartnership } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPartnershipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPersonJSON } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPersonResponse } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPersonSummary } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.activateEmployee
*/
export async function activateEmployee(
  request: {
    id: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/activate`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.createEmployee
*/
export async function createEmployee(
  request: {
    body: NewEmployee
  }
): Promise<Employee> {
  const { data: json } = await client.request<JsonOf<Employee>>({
    url: uri`/employee/employees`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NewEmployee>
  })
  return deserializeJsonEmployee(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.createSsnEmployee
*/
export async function createSsnEmployee(
  request: {
    body: NewSsnEmployee
  }
): Promise<CreateSsnEmployeeResponse> {
  const { data: json } = await client.request<JsonOf<CreateSsnEmployeeResponse>>({
    url: uri`/employee/employees/create-with-ssn`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NewSsnEmployee>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.deactivateEmployee
*/
export async function deactivateEmployee(
  request: {
    id: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/deactivate`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.deleteEmployee
*/
export async function deleteEmployee(
  request: {
    id: EmployeeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.deleteEmployeeDaycareRoles
*/
export async function deleteEmployeeDaycareRoles(
  request: {
    id: EmployeeId,
    daycareId?: DaycareId | null
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['daycareId', request.daycareId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/daycare-roles`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.deleteEmployeeScheduledDaycareRole
*/
export async function deleteEmployeeScheduledDaycareRole(
  request: {
    id: EmployeeId,
    daycareId: DaycareId
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['daycareId', request.daycareId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/scheduled-daycare-role`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.getEmployee
*/
export async function getEmployee(
  request: {
    id: EmployeeId
  }
): Promise<Employee> {
  const { data: json } = await client.request<JsonOf<Employee>>({
    url: uri`/employee/employees/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonEmployee(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.getEmployeeDetails
*/
export async function getEmployeeDetails(
  request: {
    id: EmployeeId
  }
): Promise<EmployeeWithDaycareRoles> {
  const { data: json } = await client.request<JsonOf<EmployeeWithDaycareRoles>>({
    url: uri`/employee/employees/${request.id}/details`.toString(),
    method: 'GET'
  })
  return deserializeJsonEmployeeWithDaycareRoles(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.getEmployeePreferredFirstName
*/
export async function getEmployeePreferredFirstName(): Promise<EmployeePreferredFirstName> {
  const { data: json } = await client.request<JsonOf<EmployeePreferredFirstName>>({
    url: uri`/employee/employees/preferred-first-name`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.getEmployees
*/
export async function getEmployees(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/employees`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.getFinanceDecisionHandlers
*/
export async function getFinanceDecisionHandlers(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/employees/finance-decision-handler`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.isPinLocked
*/
export async function isPinLocked(): Promise<boolean> {
  const { data: json } = await client.request<JsonOf<boolean>>({
    url: uri`/employee/employees/pin-code/is-pin-locked`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.searchEmployees
*/
export async function searchEmployees(
  request: {
    body: SearchEmployeeRequest
  }
): Promise<PagedEmployeesWithDaycareRoles> {
  const { data: json } = await client.request<JsonOf<PagedEmployeesWithDaycareRoles>>({
    url: uri`/employee/employees/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchEmployeeRequest>
  })
  return deserializeJsonPagedEmployeesWithDaycareRoles(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.setEmployeePreferredFirstName
*/
export async function setEmployeePreferredFirstName(
  request: {
    body: EmployeeSetPreferredFirstNameUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/preferred-first-name`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EmployeeSetPreferredFirstNameUpdateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.updateEmployeeGlobalRoles
*/
export async function updateEmployeeGlobalRoles(
  request: {
    id: EmployeeId,
    body: UserRole[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/global-roles`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UserRole[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.upsertEmployeeDaycareRoles
*/
export async function upsertEmployeeDaycareRoles(
  request: {
    id: EmployeeId,
    body: UpsertEmployeeDaycareRolesRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/${request.id}/daycare-roles`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpsertEmployeeDaycareRolesRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.upsertPinCode
*/
export async function upsertPinCode(
  request: {
    body: PinCode
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/employees/pin-code`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PinCode>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyController.getFamilyByPerson
*/
export async function getFamilyByPerson(
  request: {
    id: PersonId
  }
): Promise<FamilyOverview> {
  const { data: json } = await client.request<JsonOf<FamilyOverview>>({
    url: uri`/employee/family/by-adult/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonFamilyOverview(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyController.getFamilyContactSummary
*/
export async function getFamilyContactSummary(
  request: {
    childId: PersonId
  }
): Promise<FamilyContact[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<FamilyContact[]>>({
    url: uri`/employee/family/contacts`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyController.updateFamilyContactDetails
*/
export async function updateFamilyContactDetails(
  request: {
    body: FamilyContactUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/family/contacts`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FamilyContactUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyController.updateFamilyContactPriority
*/
export async function updateFamilyContactPriority(
  request: {
    body: FamilyContactPriorityUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/family/contacts/priority`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FamilyContactPriorityUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FosterParentController.createFosterParentRelationship
*/
export async function createFosterParentRelationship(
  request: {
    body: CreateFosterParentRelationshipBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/foster-parent`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateFosterParentRelationshipBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FosterParentController.deleteFosterParentRelationship
*/
export async function deleteFosterParentRelationship(
  request: {
    id: FosterParentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/foster-parent/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FosterParentController.getFosterChildren
*/
export async function getFosterChildren(
  request: {
    parentId: PersonId
  }
): Promise<FosterParentRelationship[]> {
  const { data: json } = await client.request<JsonOf<FosterParentRelationship[]>>({
    url: uri`/employee/foster-parent/by-parent/${request.parentId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFosterParentRelationship(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FosterParentController.getFosterParents
*/
export async function getFosterParents(
  request: {
    childId: PersonId
  }
): Promise<FosterParentRelationship[]> {
  const { data: json } = await client.request<JsonOf<FosterParentRelationship[]>>({
    url: uri`/employee/foster-parent/by-child/${request.childId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFosterParentRelationship(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.FosterParentController.updateFosterParentRelationshipValidity
*/
export async function updateFosterParentRelationshipValidity(
  request: {
    id: FosterParentId,
    body: DateRange
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/foster-parent/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DateRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.createParentship
*/
export async function createParentship(
  request: {
    body: ParentshipRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ParentshipRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.deleteParentship
*/
export async function deleteParentship(
  request: {
    id: ParentshipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.getParentship
*/
export async function getParentship(
  request: {
    id: ParentshipId
  }
): Promise<Parentship> {
  const { data: json } = await client.request<JsonOf<Parentship>>({
    url: uri`/employee/parentships/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonParentship(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.getParentships
*/
export async function getParentships(
  request: {
    headOfChildId?: PersonId | null,
    childId?: PersonId | null
  }
): Promise<ParentshipWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['headOfChildId', request.headOfChildId],
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<ParentshipWithPermittedActions[]>>({
    url: uri`/employee/parentships`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonParentshipWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.retryParentship
*/
export async function retryParentship(
  request: {
    id: ParentshipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships/${request.id}/retry`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.updateParentship
*/
export async function updateParentship(
  request: {
    id: ParentshipId,
    body: ParentshipUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ParentshipUpdateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.createPartnership
*/
export async function createPartnership(
  request: {
    body: PartnershipRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PartnershipRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.deletePartnership
*/
export async function deletePartnership(
  request: {
    partnershipId: PartnershipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships/${request.partnershipId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.getPartnership
*/
export async function getPartnership(
  request: {
    partnershipId: PartnershipId
  }
): Promise<Partnership> {
  const { data: json } = await client.request<JsonOf<Partnership>>({
    url: uri`/employee/partnerships/${request.partnershipId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPartnership(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.getPartnerships
*/
export async function getPartnerships(
  request: {
    personId: PersonId
  }
): Promise<PartnershipWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<PartnershipWithPermittedActions[]>>({
    url: uri`/employee/partnerships`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPartnershipWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.retryPartnership
*/
export async function retryPartnership(
  request: {
    partnershipId: PartnershipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships/${request.partnershipId}/retry`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.updatePartnership
*/
export async function updatePartnership(
  request: {
    partnershipId: PartnershipId,
    body: PartnershipUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships/${request.partnershipId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PartnershipUpdateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.addSsn
*/
export async function addSsn(
  request: {
    personId: PersonId,
    body: AddSsnRequest
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/${request.personId}/ssn`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AddSsnRequest>
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.createEmpty
*/
export async function createEmpty(): Promise<PersonIdentityResponseJSON> {
  const { data: json } = await client.request<JsonOf<PersonIdentityResponseJSON>>({
    url: uri`/employee/person`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.createPerson
*/
export async function createPerson(
  request: {
    body: CreatePersonBody
  }
): Promise<PersonId> {
  const { data: json } = await client.request<JsonOf<PersonId>>({
    url: uri`/employee/person/create`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreatePersonBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.disableSsn
*/
export async function disableSsn(
  request: {
    personId: PersonId,
    body: DisableSsnRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.personId}/ssn/disable`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DisableSsnRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.duplicatePerson
*/
export async function duplicatePerson(
  request: {
    personId: PersonId
  }
): Promise<PersonId> {
  const { data: json } = await client.request<JsonOf<PersonId>>({
    url: uri`/employee/person/${request.personId}/duplicate`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getAddressPagePdf
*/
export function getAddressPagePdf(
  request: {
    guardianId: PersonId
  }
): { url: Uri } {
  return {
    url: uri`${client.defaults.baseURL ?? ''}/employee/person/${request.guardianId}/address-page/download`
  }
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getOrCreatePersonBySsn
*/
export async function getOrCreatePersonBySsn(
  request: {
    body: GetOrCreatePersonBySsnRequest
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/details/ssn`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GetOrCreatePersonBySsnRequest>
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPerson
*/
export async function getPerson(
  request: {
    personId: PersonId
  }
): Promise<PersonResponse> {
  const { data: json } = await client.request<JsonOf<PersonResponse>>({
    url: uri`/employee/person/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonResponse(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonDependants
*/
export async function getPersonDependants(
  request: {
    personId: PersonId
  }
): Promise<PersonWithChildrenDTO[]> {
  const { data: json } = await client.request<JsonOf<PersonWithChildrenDTO[]>>({
    url: uri`/employee/person/dependants/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonWithChildrenDTO(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonGuardians
*/
export async function getPersonGuardians(
  request: {
    personId: PersonId
  }
): Promise<GuardiansResponse> {
  const { data: json } = await client.request<JsonOf<GuardiansResponse>>({
    url: uri`/employee/person/guardians/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonGuardiansResponse(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonIdentity
*/
export async function getPersonIdentity(
  request: {
    personId: PersonId
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/details/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.mergePeople
*/
export async function mergePeople(
  request: {
    body: MergeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/merge`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<MergeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.safeDeletePerson
*/
export async function safeDeletePerson(
  request: {
    personId: PersonId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.personId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.searchPerson
*/
export async function searchPerson(
  request: {
    body: SearchPersonBody
  }
): Promise<PersonSummary[]> {
  const { data: json } = await client.request<JsonOf<PersonSummary[]>>({
    url: uri`/employee/person/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchPersonBody>
  })
  return json.map(e => deserializeJsonPersonSummary(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.updateGuardianEvakaRights
*/
export async function updateGuardianEvakaRights(
  request: {
    childId: PersonId,
    body: EvakaRightsRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.childId}/evaka-rights`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EvakaRightsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.updatePersonAndFamilyFromVtj
*/
export async function updatePersonAndFamilyFromVtj(
  request: {
    personId: PersonId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.personId}/vtj-update`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.updatePersonDetails
*/
export async function updatePersonDetails(
  request: {
    personId: PersonId,
    body: PersonPatch
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/${request.personId}`.toString(),
    method: 'PATCH',
    data: request.body satisfies JsonCompatible<PersonPatch>
  })
  return deserializeJsonPersonJSON(json)
}
