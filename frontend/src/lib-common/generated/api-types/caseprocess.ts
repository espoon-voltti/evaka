// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CaseProcessId } from './shared'
import type { EvakaUser } from './user'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.caseprocess.CaseProcess
*/
export interface CaseProcess {
  archiveDurationMonths: number | null
  history: CaseProcessHistoryRow[]
  id: CaseProcessId
  migrated: boolean
  number: number
  organization: string
  processDefinitionNumber: string
  processNumber: string
  year: number
}

/**
* Generated from fi.espoo.evaka.caseprocess.CaseProcessHistoryRow
*/
export interface CaseProcessHistoryRow {
  enteredAt: HelsinkiDateTime
  enteredBy: EvakaUser
  rowIndex: number
  state: CaseProcessState
}

/**
* Generated from fi.espoo.evaka.caseprocess.CaseProcessState
*/
export type CaseProcessState =
  | 'INITIAL'
  | 'PREPARATION'
  | 'DECIDING'
  | 'COMPLETED'

/**
* Generated from fi.espoo.evaka.caseprocess.DocumentConfidentiality
*/
export interface DocumentConfidentiality {
  basis: string
  durationYears: number
}

/**
* Generated from fi.espoo.evaka.caseprocess.DocumentMetadata
*/
export interface DocumentMetadata {
  confidential: boolean | null
  confidentiality: DocumentConfidentiality | null
  createdAt: HelsinkiDateTime | null
  createdBy: EvakaUser | null
  documentId: UUID
  downloadPath: string | null
  name: string
  receivedBy: DocumentOrigin | null
  sfiDeliveries: SfiDelivery[]
}

/**
* Generated from fi.espoo.evaka.caseprocess.DocumentOrigin
*/
export type DocumentOrigin =
  | 'ELECTRONIC'
  | 'PAPER'

/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadata
*/
export interface ProcessMetadata {
  primaryDocument: DocumentMetadata
  process: CaseProcess
  processName: string | null
  secondaryDocuments: DocumentMetadata[]
}

/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataResponse
*/
export interface ProcessMetadataResponse {
  data: ProcessMetadata | null
}

/**
* Generated from fi.espoo.evaka.caseprocess.SfiDelivery
*/
export interface SfiDelivery {
  method: SfiMethod
  recipientName: string
  time: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.caseprocess.SfiMethod
*/
export type SfiMethod =
  | 'ELECTRONIC'
  | 'PAPER_MAIL'
  | 'PENDING'


export function deserializeJsonCaseProcess(json: JsonOf<CaseProcess>): CaseProcess {
  return {
    ...json,
    history: json.history.map(e => deserializeJsonCaseProcessHistoryRow(e))
  }
}


export function deserializeJsonCaseProcessHistoryRow(json: JsonOf<CaseProcessHistoryRow>): CaseProcessHistoryRow {
  return {
    ...json,
    enteredAt: HelsinkiDateTime.parseIso(json.enteredAt)
  }
}


export function deserializeJsonDocumentMetadata(json: JsonOf<DocumentMetadata>): DocumentMetadata {
  return {
    ...json,
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null,
    sfiDeliveries: json.sfiDeliveries.map(e => deserializeJsonSfiDelivery(e))
  }
}


export function deserializeJsonProcessMetadata(json: JsonOf<ProcessMetadata>): ProcessMetadata {
  return {
    ...json,
    primaryDocument: deserializeJsonDocumentMetadata(json.primaryDocument),
    process: deserializeJsonCaseProcess(json.process),
    secondaryDocuments: json.secondaryDocuments.map(e => deserializeJsonDocumentMetadata(e))
  }
}


export function deserializeJsonProcessMetadataResponse(json: JsonOf<ProcessMetadataResponse>): ProcessMetadataResponse {
  return {
    ...json,
    data: (json.data != null) ? deserializeJsonProcessMetadata(json.data) : null
  }
}


export function deserializeJsonSfiDelivery(json: JsonOf<SfiDelivery>): SfiDelivery {
  return {
    ...json,
    time: HelsinkiDateTime.parseIso(json.time)
  }
}
