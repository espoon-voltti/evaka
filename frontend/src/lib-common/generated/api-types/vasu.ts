// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { UUID } from '../../types'
import { VasuQuestion } from '../../api-types/vasu'

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
  language: VasuLanguage
  name: string
  type: CurriculumType
  valid: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.vasu.CurriculumType
*/
export type CurriculumType = 
  | 'DAYCARE'
  | 'PRESCHOOL'

/**
* Generated from fi.espoo.evaka.vasu.VasuController.EditFollowupEntryRequest
*/
export interface EditFollowupEntryRequest {
  text: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuController.GetVasuDocumentResponse
*/
export interface GetVasuDocumentResponse {
  permittedFollowupActions: UUID[]
  vasu: VasuDocument
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
  sections: VasuSection[]
}

/**
* Generated from fi.espoo.evaka.vasu.VasuDocument
*/
export interface VasuDocument {
  basics: VasuBasics
  content: VasuContent
  documentState: VasuDocumentState
  events: VasuDocumentEvent[]
  id: UUID
  language: VasuLanguage
  modifiedAt: Date
  templateName: string
  templateRange: FiniteDateRange
  type: CurriculumType
}

/**
* Generated from fi.espoo.evaka.vasu.VasuDocumentEvent
*/
export interface VasuDocumentEvent {
  created: Date
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
  id: UUID
  modifiedAt: Date
  name: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuGuardian
*/
export interface VasuGuardian {
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.vasu.VasuLanguage
*/
export type VasuLanguage = 
  | 'FI'
  | 'SV'

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
  language: VasuLanguage
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
  language: VasuLanguage
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
