// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { JsonOf } from '../../json'
import { OfficialLanguage } from './shared'
import { UUID } from '../../types'
import { VasuQuestion } from '../../api-types/vasu'
import { mapVasuQuestion } from '../../api-types/vasu'

/**
* Generated from fi.espoo.evaka.vasu.VasuController.ChangeDocumentStateRequest
*/
export interface ChangeDocumentStateRequest {
  eventType: VasuDocumentEventType
}

/**
* Generated from fi.espoo.evaka.vasu.ChildLanguage
*/
export interface ChildLanguage {
  languageSpokenAtHome: string
  nativeLanguage: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuControllerCitizen.CitizenGetVasuDocumentResponse
*/
export interface CitizenGetVasuDocumentResponse {
  guardianHasGivenPermissionToShare: boolean
  permissionToShareRequired: boolean
  vasu: VasuDocument
}

/**
* Generated from fi.espoo.evaka.vasu.VasuControllerCitizen.CitizenGetVasuDocumentSummariesResponse
*/
export interface CitizenGetVasuDocumentSummariesResponse {
  data: VasuDocumentSummary[]
  permissionToShareRequired: boolean
}

/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.CopyTemplateRequest
*/
export interface CopyTemplateRequest {
  name: string
  valid: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.vasu.VasuController.CreateDocumentRequest
*/
export interface CreateDocumentRequest {
  templateId: UUID
}

/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.CreateTemplateRequest
*/
export interface CreateTemplateRequest {
  language: OfficialLanguage
  name: string
  type: CurriculumType
  valid: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.vasu.CurriculumTemplateError
*/
export type CurriculumTemplateError =
  | 'EXPIRED_START'
  | 'EXPIRED_END'
  | 'FUTURE_START'
  | 'CURRENT_START'
  | 'CURRENT_END'
  | 'TEMPLATE_NAME'

/**
* Generated from fi.espoo.evaka.vasu.CurriculumType
*/
export const curriculumTypes = [
  'DAYCARE',
  'PRESCHOOL'
] as const

export type CurriculumType = typeof curriculumTypes[number]

/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.MigrateVasuRequest
*/
export interface MigrateVasuRequest {
  processDefinitionNumber: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuController.UpdateDocumentRequest
*/
export interface UpdateDocumentRequest {
  childLanguage: ChildLanguage | null
  content: VasuContent
}

/**
* Generated from fi.espoo.evaka.vasu.VasuBasics
*/
export interface VasuBasics {
  child: VasuChild
  childLanguage: ChildLanguage | null
  guardians: VasuGuardian[]
  placements: VasuPlacement[] | null
}

/**
* Generated from fi.espoo.evaka.vasu.VasuChild
*/
export interface VasuChild {
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuContent
*/
export interface VasuContent {
  hasDynamicFirstSection: boolean | null
  sections: VasuSection[]
}

/**
* Generated from fi.espoo.evaka.vasu.VasuDocument
*/
export interface VasuDocument {
  basics: VasuBasics
  childId: UUID
  content: VasuContent
  created: HelsinkiDateTime
  documentState: VasuDocumentState
  events: VasuDocumentEvent[]
  id: UUID
  language: OfficialLanguage
  modifiedAt: HelsinkiDateTime
  publishedAt: HelsinkiDateTime | null
  templateId: UUID
  templateName: string
  templateRange: FiniteDateRange
  type: CurriculumType
}

/**
* Generated from fi.espoo.evaka.vasu.VasuDocumentEvent
*/
export interface VasuDocumentEvent {
  created: HelsinkiDateTime
  createdBy: UUID
  eventType: VasuDocumentEventType
  id: UUID
}

/**
* Generated from fi.espoo.evaka.vasu.VasuDocumentEventType
*/
export type VasuDocumentEventType =
  | 'PUBLISHED'
  | 'MOVED_TO_READY'
  | 'RETURNED_TO_READY'
  | 'MOVED_TO_REVIEWED'
  | 'RETURNED_TO_REVIEWED'
  | 'MOVED_TO_CLOSED'

/**
* Generated from fi.espoo.evaka.vasu.VasuDocumentState
*/
export type VasuDocumentState =
  | 'DRAFT'
  | 'READY'
  | 'REVIEWED'
  | 'CLOSED'

/**
* Generated from fi.espoo.evaka.vasu.VasuDocumentSummary
*/
export interface VasuDocumentSummary {
  documentState: VasuDocumentState
  events: VasuDocumentEvent[]
  guardiansThatHaveGivenPermissionToShare: UUID[]
  id: UUID
  modifiedAt: HelsinkiDateTime
  name: string
  publishedAt: HelsinkiDateTime | null
  type: CurriculumType
}

/**
* Generated from fi.espoo.evaka.vasu.VasuController.VasuDocumentSummaryWithPermittedActions
*/
export interface VasuDocumentSummaryWithPermittedActions {
  data: VasuDocumentSummary
  permittedActions: Action.VasuDocument[]
}

/**
* Generated from fi.espoo.evaka.vasu.VasuController.VasuDocumentWithPermittedActions
*/
export interface VasuDocumentWithPermittedActions {
  data: VasuDocument
  permittedActions: Action.VasuDocument[]
}

/**
* Generated from fi.espoo.evaka.vasu.VasuGuardian
*/
export interface VasuGuardian {
  firstName: string
  hasGivenPermissionToShare: boolean
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuPlacement
*/
export interface VasuPlacement {
  groupId: UUID
  groupName: string
  range: FiniteDateRange
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuSection
*/
export interface VasuSection {
  hideBeforeReady: boolean
  name: string
  questions: VasuQuestion[]
}

/**
* Generated from fi.espoo.evaka.vasu.VasuTemplate
*/
export interface VasuTemplate {
  content: VasuContent
  documentCount: number
  id: UUID
  language: OfficialLanguage
  name: string
  type: CurriculumType
  valid: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateSummary
*/
export interface VasuTemplateSummary {
  documentCount: number
  id: UUID
  language: OfficialLanguage
  name: string
  type: CurriculumType
  valid: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateUpdate
*/
export interface VasuTemplateUpdate {
  name: string
  valid: FiniteDateRange
}


export function deserializeJsonCitizenGetVasuDocumentResponse(json: JsonOf<CitizenGetVasuDocumentResponse>): CitizenGetVasuDocumentResponse {
  return {
    ...json,
    vasu: deserializeJsonVasuDocument(json.vasu)
  }
}


export function deserializeJsonCitizenGetVasuDocumentSummariesResponse(json: JsonOf<CitizenGetVasuDocumentSummariesResponse>): CitizenGetVasuDocumentSummariesResponse {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonVasuDocumentSummary(e))
  }
}


export function deserializeJsonCopyTemplateRequest(json: JsonOf<CopyTemplateRequest>): CopyTemplateRequest {
  return {
    ...json,
    valid: FiniteDateRange.parseJson(json.valid)
  }
}


export function deserializeJsonCreateTemplateRequest(json: JsonOf<CreateTemplateRequest>): CreateTemplateRequest {
  return {
    ...json,
    valid: FiniteDateRange.parseJson(json.valid)
  }
}


export function deserializeJsonUpdateDocumentRequest(json: JsonOf<UpdateDocumentRequest>): UpdateDocumentRequest {
  return {
    ...json,
    content: deserializeJsonVasuContent(json.content)
  }
}


export function deserializeJsonVasuBasics(json: JsonOf<VasuBasics>): VasuBasics {
  return {
    ...json,
    child: deserializeJsonVasuChild(json.child),
    placements: (json.placements != null) ? json.placements.map(e => deserializeJsonVasuPlacement(e)) : null
  }
}


export function deserializeJsonVasuChild(json: JsonOf<VasuChild>): VasuChild {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonVasuContent(json: JsonOf<VasuContent>): VasuContent {
  return {
    ...json,
    sections: json.sections.map(e => deserializeJsonVasuSection(e))
  }
}


export function deserializeJsonVasuDocument(json: JsonOf<VasuDocument>): VasuDocument {
  return {
    ...json,
    basics: deserializeJsonVasuBasics(json.basics),
    content: deserializeJsonVasuContent(json.content),
    created: HelsinkiDateTime.parseIso(json.created),
    events: json.events.map(e => deserializeJsonVasuDocumentEvent(e)),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    templateRange: FiniteDateRange.parseJson(json.templateRange)
  }
}


export function deserializeJsonVasuDocumentEvent(json: JsonOf<VasuDocumentEvent>): VasuDocumentEvent {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created)
  }
}


export function deserializeJsonVasuDocumentSummary(json: JsonOf<VasuDocumentSummary>): VasuDocumentSummary {
  return {
    ...json,
    events: json.events.map(e => deserializeJsonVasuDocumentEvent(e)),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null
  }
}


export function deserializeJsonVasuDocumentSummaryWithPermittedActions(json: JsonOf<VasuDocumentSummaryWithPermittedActions>): VasuDocumentSummaryWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonVasuDocumentSummary(json.data)
  }
}


export function deserializeJsonVasuDocumentWithPermittedActions(json: JsonOf<VasuDocumentWithPermittedActions>): VasuDocumentWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonVasuDocument(json.data)
  }
}


export function deserializeJsonVasuPlacement(json: JsonOf<VasuPlacement>): VasuPlacement {
  return {
    ...json,
    range: FiniteDateRange.parseJson(json.range)
  }
}


export function deserializeJsonVasuSection(json: JsonOf<VasuSection>): VasuSection {
  return {
    ...json,
    questions: json.questions.map(e => mapVasuQuestion(e))
  }
}


export function deserializeJsonVasuTemplate(json: JsonOf<VasuTemplate>): VasuTemplate {
  return {
    ...json,
    content: deserializeJsonVasuContent(json.content),
    valid: FiniteDateRange.parseJson(json.valid)
  }
}


export function deserializeJsonVasuTemplateSummary(json: JsonOf<VasuTemplateSummary>): VasuTemplateSummary {
  return {
    ...json,
    valid: FiniteDateRange.parseJson(json.valid)
  }
}


export function deserializeJsonVasuTemplateUpdate(json: JsonOf<VasuTemplateUpdate>): VasuTemplateUpdate {
  return {
    ...json,
    valid: FiniteDateRange.parseJson(json.valid)
  }
}
