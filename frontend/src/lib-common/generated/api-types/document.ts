// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { ChildDocumentDecisionId } from './shared'
import type { ChildDocumentId } from './shared'
import DateRange from '../../date-range'
import type { DocumentConfidentiality } from './process'
import type { DocumentTemplateId } from './shared'
import type { EmployeeId } from './shared'
import type { EvakaUser } from './user'
import type { EvakaUserId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { PersonId } from './shared'
import type { PlacementType } from './placement'
import type { UiLanguage } from './shared'

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.AcceptChildDocumentDecisionRequest
*/
export interface AcceptChildDocumentDecisionRequest {
  validity: DateRange
}


export namespace AnsweredQuestion {
  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.CheckboxAnswer
  */
  export interface CheckboxAnswer {
    type: 'CHECKBOX'
    answer: boolean
    questionId: string
  }

  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.CheckboxGroupAnswer
  */
  export interface CheckboxGroupAnswer {
    type: 'CHECKBOX_GROUP'
    answer: CheckboxGroupAnswerContent[]
    questionId: string
  }

  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.DateAnswer
  */
  export interface DateAnswer {
    type: 'DATE'
    answer: LocalDate | null
    questionId: string
  }

  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.GroupedTextFieldsAnswer
  */
  export interface GroupedTextFieldsAnswer {
    type: 'GROUPED_TEXT_FIELDS'
    answer: string[][]
    questionId: string
  }

  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.RadioButtonGroupAnswer
  */
  export interface RadioButtonGroupAnswer {
    type: 'RADIO_BUTTON_GROUP'
    answer: string | null
    questionId: string
  }

  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.StaticTextDisplayAnswer
  */
  export interface StaticTextDisplayAnswer {
    type: 'STATIC_TEXT_DISPLAY'
    answer: never | null
    questionId: string
  }

  /**
  * Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion.TextAnswer
  */
  export interface TextAnswer {
    type: 'TEXT'
    answer: string
    questionId: string
  }
}

/**
* Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion
*/
export type AnsweredQuestion = AnsweredQuestion.CheckboxAnswer | AnsweredQuestion.CheckboxGroupAnswer | AnsweredQuestion.DateAnswer | AnsweredQuestion.GroupedTextFieldsAnswer | AnsweredQuestion.RadioButtonGroupAnswer | AnsweredQuestion.StaticTextDisplayAnswer | AnsweredQuestion.TextAnswer


/**
* Generated from fi.espoo.evaka.document.childdocument.CheckboxGroupAnswerContent
*/
export interface CheckboxGroupAnswerContent {
  extra: string
  optionId: string
}

/**
* Generated from fi.espoo.evaka.document.CheckboxGroupQuestionOption
*/
export interface CheckboxGroupQuestionOption {
  id: string
  label: string
  withText: boolean
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildBasics
*/
export interface ChildBasics {
  dateOfBirth: LocalDate | null
  firstName: string
  id: PersonId
  lastName: string
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentCitizenDetails
*/
export interface ChildDocumentCitizenDetails {
  child: ChildBasics
  content: DocumentContent
  decision: ChildDocumentDecision | null
  downloadable: boolean
  id: ChildDocumentId
  publishedAt: HelsinkiDateTime | null
  status: DocumentStatus
  template: DocumentTemplate
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentCitizenSummary
*/
export interface ChildDocumentCitizenSummary {
  answeredAt: HelsinkiDateTime | null
  answeredBy: EvakaUser | null
  child: ChildBasics
  decision: ChildDocumentDecision | null
  id: ChildDocumentId
  publishedAt: HelsinkiDateTime
  status: DocumentStatus
  templateName: string
  type: DocumentType
  unread: boolean
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentCreateRequest
*/
export interface ChildDocumentCreateRequest {
  childId: PersonId
  templateId: DocumentTemplateId
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentDecision
*/
export interface ChildDocumentDecision {
  createdAt: HelsinkiDateTime
  decisionNumber: number
  id: ChildDocumentDecisionId
  status: ChildDocumentDecisionStatus
  validity: DateRange | null
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentDecisionStatus
*/
export type ChildDocumentDecisionStatus =
  | 'ACCEPTED'
  | 'REJECTED'
  | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentDetails
*/
export interface ChildDocumentDetails {
  archivedAt: HelsinkiDateTime | null
  child: ChildBasics
  content: DocumentContent
  decision: ChildDocumentDecision | null
  decisionMaker: EmployeeId | null
  id: ChildDocumentId
  pdfAvailable: boolean
  publishedAt: HelsinkiDateTime | null
  publishedContent: DocumentContent | null
  status: DocumentStatus
  template: DocumentTemplate
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentOrDecisionStatus
*/
export type ChildDocumentOrDecisionStatus =
  | 'DRAFT'
  | 'PREPARED'
  | 'CITIZEN_DRAFT'
  | 'DECISION_PROPOSAL'
  | 'COMPLETED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentSummary
*/
export interface ChildDocumentSummary {
  answeredAt: HelsinkiDateTime | null
  answeredBy: EvakaUser | null
  childFirstName: string
  childLastName: string
  decision: ChildDocumentDecision | null
  decisionMaker: EvakaUser | null
  id: ChildDocumentId
  modifiedAt: HelsinkiDateTime
  publishedAt: HelsinkiDateTime | null
  status: DocumentStatus
  templateId: DocumentTemplateId
  templateName: string
  type: DocumentType
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.ChildDocumentSummaryWithPermittedActions
*/
export interface ChildDocumentSummaryWithPermittedActions {
  data: ChildDocumentSummary
  permittedActions: Action.ChildDocument[]
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentWithPermittedActions
*/
export interface ChildDocumentWithPermittedActions {
  data: ChildDocumentDetails
  permittedActions: Action.ChildDocument[]
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentsCreateRequest
*/
export interface ChildDocumentsCreateRequest {
  childIds: PersonId[]
  templateId: DocumentTemplateId
}

/**
* Generated from fi.espoo.evaka.document.childdocument.DocumentContent
*/
export interface DocumentContent {
  answers: AnsweredQuestion[]
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.DocumentLockResponse
*/
export interface DocumentLockResponse {
  currentLock: DocumentWriteLock
  lockTakenSuccessfully: boolean
}

/**
* Generated from fi.espoo.evaka.document.childdocument.DocumentStatus
*/
export type DocumentStatus =
  | 'DRAFT'
  | 'PREPARED'
  | 'CITIZEN_DRAFT'
  | 'DECISION_PROPOSAL'
  | 'COMPLETED'

/**
* Generated from fi.espoo.evaka.document.DocumentTemplate
*/
export interface DocumentTemplate {
  archiveDurationMonths: number | null
  archiveExternally: boolean
  confidentiality: DocumentConfidentiality | null
  content: DocumentTemplateContent
  id: DocumentTemplateId
  language: UiLanguage
  legalBasis: string
  name: string
  placementTypes: PlacementType[]
  processDefinitionNumber: string | null
  published: boolean
  type: DocumentType
  validity: DateRange
}


export namespace DocumentTemplateBasicsRequest {
  /**
  * Generated from fi.espoo.evaka.document.DocumentTemplateBasicsRequest.ArchivedExternally
  */
  export interface ArchivedExternally {
    templateType: 'ARCHIVED_EXTERNALLY'
    archiveDurationMonths: number
    archiveExternally: boolean
    confidentiality: DocumentConfidentiality | null
    language: UiLanguage
    legalBasis: string
    name: string
    placementTypes: PlacementType[]
    processDefinitionNumber: string
    type: DocumentType
    validity: DateRange
  }

  /**
  * Generated from fi.espoo.evaka.document.DocumentTemplateBasicsRequest.Regular
  */
  export interface Regular {
    templateType: 'REGULAR'
    archiveDurationMonths: number | null
    archiveExternally: boolean
    confidentiality: DocumentConfidentiality | null
    language: UiLanguage
    legalBasis: string
    name: string
    placementTypes: PlacementType[]
    processDefinitionNumber: string | null
    type: DocumentType
    validity: DateRange
  }
}

/**
* Generated from fi.espoo.evaka.document.DocumentTemplateBasicsRequest
*/
export type DocumentTemplateBasicsRequest = DocumentTemplateBasicsRequest.ArchivedExternally | DocumentTemplateBasicsRequest.Regular


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateContent
*/
export interface DocumentTemplateContent {
  sections: Section[]
}

/**
* Generated from fi.espoo.evaka.document.DocumentTemplateSummary
*/
export interface DocumentTemplateSummary {
  documentCount: number
  id: DocumentTemplateId
  language: UiLanguage
  name: string
  placementTypes: PlacementType[]
  published: boolean
  type: DocumentType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.document.DocumentType
*/
export const documentTypes = [
  'PEDAGOGICAL_REPORT',
  'PEDAGOGICAL_ASSESSMENT',
  'HOJKS',
  'MIGRATED_VASU',
  'MIGRATED_LEOPS',
  'VASU',
  'LEOPS',
  'CITIZEN_BASIC',
  'OTHER_DECISION',
  'OTHER'
] as const

export type DocumentType = typeof documentTypes[number]

/**
* Generated from fi.espoo.evaka.document.childdocument.DocumentWriteLock
*/
export interface DocumentWriteLock {
  modifiedBy: EvakaUserId
  modifiedByName: string
  opensAt: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.document.ExportedDocumentTemplate
*/
export interface ExportedDocumentTemplate {
  archiveDurationMonths: number | null
  archiveExternally: boolean
  confidentiality: DocumentConfidentiality | null
  content: DocumentTemplateContent
  language: UiLanguage
  legalBasis: string
  name: string
  placementTypes: PlacementType[]
  processDefinitionNumber: string | null
  type: DocumentType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.ProposeChildDocumentDecisionRequest
*/
export interface ProposeChildDocumentDecisionRequest {
  decisionMaker: EmployeeId
}


export namespace Question {
  /**
  * Generated from fi.espoo.evaka.document.Question.CheckboxGroupQuestion
  */
  export interface CheckboxGroupQuestion {
    type: 'CHECKBOX_GROUP'
    id: string
    infoText: string
    label: string
    options: CheckboxGroupQuestionOption[]
  }

  /**
  * Generated from fi.espoo.evaka.document.Question.CheckboxQuestion
  */
  export interface CheckboxQuestion {
    type: 'CHECKBOX'
    id: string
    infoText: string
    label: string
  }

  /**
  * Generated from fi.espoo.evaka.document.Question.DateQuestion
  */
  export interface DateQuestion {
    type: 'DATE'
    id: string
    infoText: string
    label: string
  }

  /**
  * Generated from fi.espoo.evaka.document.Question.GroupedTextFieldsQuestion
  */
  export interface GroupedTextFieldsQuestion {
    type: 'GROUPED_TEXT_FIELDS'
    allowMultipleRows: boolean
    fieldLabels: string[]
    id: string
    infoText: string
    label: string
  }

  /**
  * Generated from fi.espoo.evaka.document.Question.RadioButtonGroupQuestion
  */
  export interface RadioButtonGroupQuestion {
    type: 'RADIO_BUTTON_GROUP'
    id: string
    infoText: string
    label: string
    options: RadioButtonGroupQuestionOption[]
  }

  /**
  * Generated from fi.espoo.evaka.document.Question.StaticTextDisplayQuestion
  */
  export interface StaticTextDisplayQuestion {
    type: 'STATIC_TEXT_DISPLAY'
    id: string
    infoText: string
    label: string
    text: string
  }

  /**
  * Generated from fi.espoo.evaka.document.Question.TextQuestion
  */
  export interface TextQuestion {
    type: 'TEXT'
    id: string
    infoText: string
    label: string
    multiline: boolean
  }
}

/**
* Generated from fi.espoo.evaka.document.Question
*/
export type Question = Question.CheckboxGroupQuestion | Question.CheckboxQuestion | Question.DateQuestion | Question.GroupedTextFieldsQuestion | Question.RadioButtonGroupQuestion | Question.StaticTextDisplayQuestion | Question.TextQuestion


/**
* Generated from fi.espoo.evaka.document.QuestionType
*/
export const questionTypes = [
  'TEXT',
  'CHECKBOX',
  'CHECKBOX_GROUP',
  'RADIO_BUTTON_GROUP',
  'STATIC_TEXT_DISPLAY',
  'DATE',
  'GROUPED_TEXT_FIELDS'
] as const

export type QuestionType = typeof questionTypes[number]

/**
* Generated from fi.espoo.evaka.document.RadioButtonGroupQuestionOption
*/
export interface RadioButtonGroupQuestionOption {
  id: string
  label: string
}

/**
* Generated from fi.espoo.evaka.document.Section
*/
export interface Section {
  id: string
  infoText: string
  label: string
  questions: Question[]
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.StatusChangeRequest
*/
export interface StatusChangeRequest {
  newStatus: DocumentStatus
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentControllerCitizen.UpdateChildDocumentRequest
*/
export interface UpdateChildDocumentRequest {
  content: DocumentContent
  status: DocumentStatus
}


export function deserializeJsonAcceptChildDocumentDecisionRequest(json: JsonOf<AcceptChildDocumentDecisionRequest>): AcceptChildDocumentDecisionRequest {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}



export function deserializeJsonAnsweredQuestionDateAnswer(json: JsonOf<AnsweredQuestion.DateAnswer>): AnsweredQuestion.DateAnswer {
  return {
    ...json,
    answer: (json.answer != null) ? LocalDate.parseIso(json.answer) : null
  }
}
export function deserializeJsonAnsweredQuestion(json: JsonOf<AnsweredQuestion>): AnsweredQuestion {
  switch (json.type) {
    case 'DATE': return deserializeJsonAnsweredQuestionDateAnswer(json)
    default: return json
  }
}


export function deserializeJsonChildBasics(json: JsonOf<ChildBasics>): ChildBasics {
  return {
    ...json,
    dateOfBirth: (json.dateOfBirth != null) ? LocalDate.parseIso(json.dateOfBirth) : null
  }
}


export function deserializeJsonChildDocumentCitizenDetails(json: JsonOf<ChildDocumentCitizenDetails>): ChildDocumentCitizenDetails {
  return {
    ...json,
    child: deserializeJsonChildBasics(json.child),
    content: deserializeJsonDocumentContent(json.content),
    decision: (json.decision != null) ? deserializeJsonChildDocumentDecision(json.decision) : null,
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    template: deserializeJsonDocumentTemplate(json.template)
  }
}


export function deserializeJsonChildDocumentCitizenSummary(json: JsonOf<ChildDocumentCitizenSummary>): ChildDocumentCitizenSummary {
  return {
    ...json,
    answeredAt: (json.answeredAt != null) ? HelsinkiDateTime.parseIso(json.answeredAt) : null,
    child: deserializeJsonChildBasics(json.child),
    decision: (json.decision != null) ? deserializeJsonChildDocumentDecision(json.decision) : null,
    publishedAt: HelsinkiDateTime.parseIso(json.publishedAt)
  }
}


export function deserializeJsonChildDocumentDecision(json: JsonOf<ChildDocumentDecision>): ChildDocumentDecision {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    validity: (json.validity != null) ? DateRange.parseJson(json.validity) : null
  }
}


export function deserializeJsonChildDocumentDetails(json: JsonOf<ChildDocumentDetails>): ChildDocumentDetails {
  return {
    ...json,
    archivedAt: (json.archivedAt != null) ? HelsinkiDateTime.parseIso(json.archivedAt) : null,
    child: deserializeJsonChildBasics(json.child),
    content: deserializeJsonDocumentContent(json.content),
    decision: (json.decision != null) ? deserializeJsonChildDocumentDecision(json.decision) : null,
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    publishedContent: (json.publishedContent != null) ? deserializeJsonDocumentContent(json.publishedContent) : null,
    template: deserializeJsonDocumentTemplate(json.template)
  }
}


export function deserializeJsonChildDocumentSummary(json: JsonOf<ChildDocumentSummary>): ChildDocumentSummary {
  return {
    ...json,
    answeredAt: (json.answeredAt != null) ? HelsinkiDateTime.parseIso(json.answeredAt) : null,
    decision: (json.decision != null) ? deserializeJsonChildDocumentDecision(json.decision) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null
  }
}


export function deserializeJsonChildDocumentSummaryWithPermittedActions(json: JsonOf<ChildDocumentSummaryWithPermittedActions>): ChildDocumentSummaryWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonChildDocumentSummary(json.data)
  }
}


export function deserializeJsonChildDocumentWithPermittedActions(json: JsonOf<ChildDocumentWithPermittedActions>): ChildDocumentWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonChildDocumentDetails(json.data)
  }
}


export function deserializeJsonDocumentContent(json: JsonOf<DocumentContent>): DocumentContent {
  return {
    ...json,
    answers: json.answers.map(e => deserializeJsonAnsweredQuestion(e))
  }
}


export function deserializeJsonDocumentLockResponse(json: JsonOf<DocumentLockResponse>): DocumentLockResponse {
  return {
    ...json,
    currentLock: deserializeJsonDocumentWriteLock(json.currentLock)
  }
}


export function deserializeJsonDocumentTemplate(json: JsonOf<DocumentTemplate>): DocumentTemplate {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}



export function deserializeJsonDocumentTemplateBasicsRequestArchivedExternally(json: JsonOf<DocumentTemplateBasicsRequest.ArchivedExternally>): DocumentTemplateBasicsRequest.ArchivedExternally {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}

export function deserializeJsonDocumentTemplateBasicsRequestRegular(json: JsonOf<DocumentTemplateBasicsRequest.Regular>): DocumentTemplateBasicsRequest.Regular {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}
export function deserializeJsonDocumentTemplateBasicsRequest(json: JsonOf<DocumentTemplateBasicsRequest>): DocumentTemplateBasicsRequest {
  switch (json.templateType) {
    case 'ARCHIVED_EXTERNALLY': return deserializeJsonDocumentTemplateBasicsRequestArchivedExternally(json)
    case 'REGULAR': return deserializeJsonDocumentTemplateBasicsRequestRegular(json)
    default: return json
  }
}


export function deserializeJsonDocumentTemplateSummary(json: JsonOf<DocumentTemplateSummary>): DocumentTemplateSummary {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonDocumentWriteLock(json: JsonOf<DocumentWriteLock>): DocumentWriteLock {
  return {
    ...json,
    opensAt: HelsinkiDateTime.parseIso(json.opensAt)
  }
}


export function deserializeJsonExportedDocumentTemplate(json: JsonOf<ExportedDocumentTemplate>): ExportedDocumentTemplate {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonUpdateChildDocumentRequest(json: JsonOf<UpdateChildDocumentRequest>): UpdateChildDocumentRequest {
  return {
    ...json,
    content: deserializeJsonDocumentContent(json.content)
  }
}
