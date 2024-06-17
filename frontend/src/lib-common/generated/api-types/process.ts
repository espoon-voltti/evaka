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
* Generated from fi.espoo.evaka.process.ProcessMetadataController.DocumentMetadata
*/
export interface DocumentMetadata {
  confidential: boolean
  createdAt: HelsinkiDateTime | null
  createdBy: EvakaUser | null
  downloadPath: string | null
  name: string
}

/**
* Generated from fi.espoo.evaka.process.ProcessMetadataController.ProcessMetadata
*/
export interface ProcessMetadata {
  primaryDocument: DocumentMetadata
  process: ArchivedProcess
  secondaryDocuments: DocumentMetadata[]
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


export function deserializeJsonDocumentMetadata(json: JsonOf<DocumentMetadata>): DocumentMetadata {
  return {
    ...json,
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null
  }
}


export function deserializeJsonProcessMetadata(json: JsonOf<ProcessMetadata>): ProcessMetadata {
  return {
    ...json,
    primaryDocument: deserializeJsonDocumentMetadata(json.primaryDocument),
    process: deserializeJsonArchivedProcess(json.process),
    secondaryDocuments: json.secondaryDocuments.map(e => deserializeJsonDocumentMetadata(e))
  }
}


export function deserializeJsonProcessMetadataResponse(json: JsonOf<ProcessMetadataResponse>): ProcessMetadataResponse {
  return {
    ...json,
    data: (json.data != null) ? deserializeJsonProcessMetadata(json.data) : null
  }
}
