// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { JsonOf } from '../../json'
import { ServiceNeed } from './serviceneed'
import { UUID } from '../../types'
import { deserializeJsonServiceNeed } from './serviceneed'

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareChild
*/
export interface BackupCareChild {
  birthDate: LocalDate
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareCreateResponse
*/
export interface BackupCareCreateResponse {
  id: UUID
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareGroup
*/
export interface BackupCareGroup {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareUnit
*/
export interface BackupCareUnit {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.backupcare.BackupCareUpdateRequest
*/
export interface BackupCareUpdateRequest {
  groupId: UUID | null
  period: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.backupcare.ChildBackupCare
*/
export interface ChildBackupCare {
  group: BackupCareGroup | null
  id: UUID
  period: FiniteDateRange
  unit: BackupCareUnit
}

/**
* Generated from fi.espoo.evaka.backupcare.ChildBackupCareResponse
*/
export interface ChildBackupCareResponse {
  backupCare: ChildBackupCare
  permittedActions: Action.BackupCare[]
}

/**
* Generated from fi.espoo.evaka.backupcare.ChildBackupCaresResponse
*/
export interface ChildBackupCaresResponse {
  backupCares: ChildBackupCareResponse[]
}

/**
* Generated from fi.espoo.evaka.backupcare.NewBackupCare
*/
export interface NewBackupCare {
  groupId: UUID | null
  period: FiniteDateRange
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.backupcare.UnitBackupCare
*/
export interface UnitBackupCare {
  child: BackupCareChild
  fromUnits: string[]
  group: BackupCareGroup | null
  id: UUID
  missingServiceNeedDays: number
  period: FiniteDateRange
  serviceNeeds: ServiceNeed[]
}

/**
* Generated from fi.espoo.evaka.backupcare.UnitBackupCaresResponse
*/
export interface UnitBackupCaresResponse {
  backupCares: UnitBackupCare[]
}


export function deserializeJsonBackupCareChild(json: JsonOf<BackupCareChild>): BackupCareChild {
  return {
    ...json,
    birthDate: LocalDate.parseIso(json.birthDate)
  }
}


export function deserializeJsonBackupCareUpdateRequest(json: JsonOf<BackupCareUpdateRequest>): BackupCareUpdateRequest {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonChildBackupCare(json: JsonOf<ChildBackupCare>): ChildBackupCare {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonChildBackupCareResponse(json: JsonOf<ChildBackupCareResponse>): ChildBackupCareResponse {
  return {
    ...json,
    backupCare: deserializeJsonChildBackupCare(json.backupCare)
  }
}


export function deserializeJsonChildBackupCaresResponse(json: JsonOf<ChildBackupCaresResponse>): ChildBackupCaresResponse {
  return {
    ...json,
    backupCares: json.backupCares.map(e => deserializeJsonChildBackupCareResponse(e))
  }
}


export function deserializeJsonNewBackupCare(json: JsonOf<NewBackupCare>): NewBackupCare {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonUnitBackupCare(json: JsonOf<UnitBackupCare>): UnitBackupCare {
  return {
    ...json,
    child: deserializeJsonBackupCareChild(json.child),
    period: FiniteDateRange.parseJson(json.period),
    serviceNeeds: json.serviceNeeds.map(e => deserializeJsonServiceNeed(e))
  }
}


export function deserializeJsonUnitBackupCaresResponse(json: JsonOf<UnitBackupCaresResponse>): UnitBackupCaresResponse {
  return {
    ...json,
    backupCares: json.backupCares.map(e => deserializeJsonUnitBackupCare(e))
  }
}
