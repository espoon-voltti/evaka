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
* Generated from fi.espoo.evaka.process.ProcessMetadataController.ChildDocumentMetadata
*/
export interface ChildDocumentMetadata {
  confidentialDocument: boolean
  documentCreatedAt: HelsinkiDateTime | null
  documentCreatedBy: EmployeeBasics | null
  documentName: string
  downloadable: boolean
  process: ArchivedProcess
}

/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.ChildDocumentMetadataResponse
*/
export interface ChildDocumentMetadataResponse {
  data: ChildDocumentMetadata | null
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


export function deserializeJsonChildDocumentMetadata(json: JsonOf<ChildDocumentMetadata>): ChildDocumentMetadata {
  return {
    ...json,
    documentCreatedAt: (json.documentCreatedAt != null) ? HelsinkiDateTime.parseIso(json.documentCreatedAt) : null,
    process: deserializeJsonArchivedProcess(json.process)
  }
}


export function deserializeJsonChildDocumentMetadataResponse(json: JsonOf<ChildDocumentMetadataResponse>): ChildDocumentMetadataResponse {
  return {
    ...json,
    data: (json.data != null) ? deserializeJsonChildDocumentMetadata(json.data) : null
  }
}
