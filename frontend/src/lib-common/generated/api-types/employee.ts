// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DaycareId } from './shared'
import type { EmployeeId } from './shared'
import type { GroupId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { MobileDevice } from './pairing'
import type { ProviderType } from './daycare'
import type { UserRole } from './shared'

/**
* Generated from evaka.core.employee.EmployeeController.CreateSsnEmployeeResponse
*/
export interface CreateSsnEmployeeResponse {
  id: EmployeeId
}

/**
* Generated from evaka.core.employee.DaycareGroupRole
*/
export interface DaycareGroupRole {
  daycareId: DaycareId
  daycareName: string
  groupId: GroupId
  groupName: string
}

/**
* Generated from evaka.core.employee.DaycareRole
*/
export interface DaycareRole {
  daycareId: DaycareId
  daycareName: string
  endDate: LocalDate | null
  role: UserRole
}

/**
* Generated from evaka.core.employee.Employee
*/
export interface Employee {
  active: boolean
  created: HelsinkiDateTime
  email: string | null
  externalId: string | null
  firstName: string
  hasSsn: boolean
  id: EmployeeId
  lastLogin: HelsinkiDateTime | null
  lastName: string
  preferredFirstName: string | null
  temporaryInUnitId: DaycareId | null
  updated: HelsinkiDateTime | null
}

/**
* Generated from evaka.core.employee.EmployeeController.EmployeeEmailRequest
*/
export interface EmployeeEmailRequest {
  email: string | null
}

/**
* Generated from evaka.core.employee.EmployeeController.EmployeePreferredFirstName
*/
export interface EmployeePreferredFirstName {
  preferredFirstName: string | null
  preferredFirstNameOptions: string[]
}

/**
* Generated from evaka.core.employee.EmployeeController.EmployeeSetPreferredFirstNameUpdateRequest
*/
export interface EmployeeSetPreferredFirstNameUpdateRequest {
  preferredFirstName: string | null
}

/**
* Generated from evaka.core.employee.EmployeeWithDaycareRoles
*/
export interface EmployeeWithDaycareRoles {
  active: boolean
  canSetAsAdmin: boolean
  created: HelsinkiDateTime
  daycareGroupRoles: DaycareGroupRole[]
  daycareRoles: DaycareRole[]
  email: string | null
  employeeNumber: string | null
  externalId: string | null
  firstName: string
  globalRoles: UserRole[]
  hasSsn: boolean
  id: EmployeeId
  lastLogin: HelsinkiDateTime | null
  lastName: string
  personalMobileDevices: MobileDevice[]
  scheduledDaycareRoles: ScheduledDaycareRole[]
  temporaryUnitName: string | null
  updated: HelsinkiDateTime | null
}

/**
* Generated from evaka.core.employee.NewSsnEmployee
*/
export interface NewSsnEmployee {
  email: string | null
  firstName: string
  lastName: string
  ssn: string
}

/**
* Generated from evaka.core.employee.PinCode
*/
export interface PinCode {
  pin: string
}

/**
* Generated from evaka.core.employee.ScheduledDaycareRole
*/
export interface ScheduledDaycareRole {
  daycareId: DaycareId
  daycareName: string
  endDate: LocalDate | null
  role: UserRole
  startDate: LocalDate
}

/**
* Generated from evaka.core.employee.SearchEmployeeRequest
*/
export interface SearchEmployeeRequest {
  globalRoles: UserRole[] | null
  hideDeactivated: boolean | null
  searchTerm: string | null
  unitProviderTypes: ProviderType[] | null
  unitRoles: UserRole[] | null
}

/**
* Generated from evaka.core.employee.TemporaryEmployee
*/
export interface TemporaryEmployee {
  firstName: string
  groupIds: GroupId[]
  hasStaffOccupancyEffect: boolean
  lastName: string
  pinCode: PinCode | null
}

/**
* Generated from evaka.core.employee.TemporaryEmployeeResponse
*/
export interface TemporaryEmployeeResponse {
  firstName: string
  groupIds: GroupId[]
  hasStaffOccupancyEffect: boolean
  lastName: string
}

/**
* Generated from evaka.core.employee.EmployeeController.UpsertEmployeeDaycareRolesRequest
*/
export interface UpsertEmployeeDaycareRolesRequest {
  daycareIds: DaycareId[]
  endDate: LocalDate | null
  role: UserRole
  startDate: LocalDate
}


export function deserializeJsonDaycareRole(json: JsonOf<DaycareRole>): DaycareRole {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null
  }
}


export function deserializeJsonEmployee(json: JsonOf<Employee>): Employee {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    lastLogin: (json.lastLogin != null) ? HelsinkiDateTime.parseIso(json.lastLogin) : null,
    updated: (json.updated != null) ? HelsinkiDateTime.parseIso(json.updated) : null
  }
}


export function deserializeJsonEmployeeWithDaycareRoles(json: JsonOf<EmployeeWithDaycareRoles>): EmployeeWithDaycareRoles {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    daycareRoles: json.daycareRoles.map(e => deserializeJsonDaycareRole(e)),
    lastLogin: (json.lastLogin != null) ? HelsinkiDateTime.parseIso(json.lastLogin) : null,
    scheduledDaycareRoles: json.scheduledDaycareRoles.map(e => deserializeJsonScheduledDaycareRole(e)),
    updated: (json.updated != null) ? HelsinkiDateTime.parseIso(json.updated) : null
  }
}


export function deserializeJsonScheduledDaycareRole(json: JsonOf<ScheduledDaycareRole>): ScheduledDaycareRole {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonUpsertEmployeeDaycareRolesRequest(json: JsonOf<UpsertEmployeeDaycareRolesRequest>): UpsertEmployeeDaycareRolesRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}
