// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { EvakaUser } from './user'
import { JsonOf } from '../../json'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.process.ArchivedProcess
*/
export interface ArchivedProcess {
  archiveDurationMonths: number
  history: ArchivedProcessHistoryRow[]
  id: UUID
  number: number
  organization: string
  processDefinitionNumber: string
  processNumber: string
  year: number
}

/**
* Generated from fi.espoo.evaka.process.ArchivedProcessHistoryRow
*/
export interface ArchivedProcessHistoryRow {
  enteredAt: HelsinkiDateTime
  enteredBy: EvakaUser
  rowIndex: number
  state: ArchivedProcessState
}

/**
* Generated from fi.espoo.evaka.process.ArchivedProcessState
*/
export type ArchivedProcessState =
  | 'INITIAL'
  | 'PREPARATION'
  | 'DECIDING'
  | 'COMPLETED'

/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.Document
*/
export interface Document {
  confidential: boolean
  createdAt: HelsinkiDateTime | null
  createdBy: EmployeeBasics | null
  downloadPath: string | null
  name: string
}

/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.EmployeeBasics
*/
export interface EmployeeBasics {
  email: string | null
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.ProcessMetadata
*/
export interface ProcessMetadata {
  primaryDocument: Document
  process: ArchivedProcess
}

/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.ProcessMetadataResponse
*/
export interface ProcessMetadataResponse {
  data: ProcessMetadata | null
}


export function deserializeJsonArchivedProcess(json: JsonOf<ArchivedProcess>): ArchivedProcess {
  return {
    ...json,
    history: json.history.map(e => deserializeJsonArchivedProcessHistoryRow(e))
  }
}


export function deserializeJsonArchivedProcessHistoryRow(json: JsonOf<ArchivedProcessHistoryRow>): ArchivedProcessHistoryRow {
  return {
    ...json,
    enteredAt: HelsinkiDateTime.parseIso(json.enteredAt)
  }
}


export function deserializeJsonDocument(json: JsonOf<Document>): Document {
  return {
    ...json,
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null
  }
}


export function deserializeJsonProcessMetadata(json: JsonOf<ProcessMetadata>): ProcessMetadata {
  return {
    ...json,
    primaryDocument: deserializeJsonDocument(json.primaryDocument),
    process: deserializeJsonArchivedProcess(json.process)
  }
}


export function deserializeJsonProcessMetadataResponse(json: JsonOf<ProcessMetadataResponse>): ProcessMetadataResponse {
  return {
    ...json,
    data: (json.data != null) ? deserializeJsonProcessMetadata(json.data) : null
  }
}
