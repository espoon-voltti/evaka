// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationType } from './application'
import type { CaseProcessId } from './shared'
import type { DecisionType } from './decision'
import type { EvakaUser } from './user'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import type { UUID } from '../../types'

/**
* Generated from evaka.core.caseprocess.CaseProcess
*/
export interface CaseProcess {
  archiveDurationMonths: number | null
  caseIdentifier: string
  history: CaseProcessHistoryRow[]
  id: CaseProcessId
  migrated: boolean
  number: number
  organization: string
  processDefinitionNumber: string
  year: number
}

/**
* Generated from evaka.core.caseprocess.CaseProcessHistoryRow
*/
export interface CaseProcessHistoryRow {
  enteredAt: HelsinkiDateTime
  enteredBy: EvakaUser
  rowIndex: number
  state: CaseProcessState
}

/**
* Generated from evaka.core.caseprocess.CaseProcessState
*/
export type CaseProcessState =
  | 'INITIAL'
  | 'PREPARATION'
  | 'DECIDING'
  | 'COMPLETED'

/**
* Generated from evaka.core.caseprocess.DocumentConfidentiality
*/
export interface DocumentConfidentiality {
  basis: string
  durationYears: number
}

/**
* Generated from evaka.core.caseprocess.DocumentMetadata
*/
export interface DocumentMetadata {
  applicationType: ApplicationType | null
  confidential: boolean | null
  confidentiality: DocumentConfidentiality | null
  createdAtDate: LocalDate | null
  createdAtTime: LocalTime | null
  createdBy: EvakaUser | null
  decisionType: DecisionType | null
  documentId: UUID
  downloadPath: string | null
  name: string
  publishedVersions: DocumentVersion[] | null
  receivedBy: DocumentOrigin | null
  sfiDeliveries: SfiDelivery[]
}

/**
* Generated from evaka.core.caseprocess.DocumentOrigin
*/
export type DocumentOrigin =
  | 'ELECTRONIC'
  | 'PAPER'

/**
* Generated from evaka.core.caseprocess.DocumentVersion
*/
export interface DocumentVersion {
  createdAt: HelsinkiDateTime
  createdBy: EvakaUser
  downloadPath: string | null
  versionNumber: number
}

/**
* Generated from evaka.core.caseprocess.ProcessMetadata
*/
export interface ProcessMetadata {
  primaryDocument: DocumentMetadata
  process: CaseProcess
  processName: string | null
  secondaryDocuments: DocumentMetadata[]
}

/**
* Generated from evaka.core.caseprocess.ProcessMetadataResponse
*/
export interface ProcessMetadataResponse {
  data: ProcessMetadata | null
}

/**
* Generated from evaka.core.caseprocess.SfiDelivery
*/
export interface SfiDelivery {
  method: SfiMethod
  recipientName: string
  time: HelsinkiDateTime
}

/**
* Generated from evaka.core.caseprocess.SfiMethod
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
    createdAtDate: (json.createdAtDate != null) ? LocalDate.parseIso(json.createdAtDate) : null,
    createdAtTime: (json.createdAtTime != null) ? LocalTime.parseIso(json.createdAtTime) : null,
    publishedVersions: (json.publishedVersions != null) ? json.publishedVersions.map(e => deserializeJsonDocumentVersion(e)) : null,
    sfiDeliveries: json.sfiDeliveries.map(e => deserializeJsonSfiDelivery(e))
  }
}


export function deserializeJsonDocumentVersion(json: JsonOf<DocumentVersion>): DocumentVersion {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
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
