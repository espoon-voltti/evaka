// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from '../../date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { JsonOf } from '../../json'
import { UUID } from '../../types'


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
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentCitizenDetails
*/
export interface ChildDocumentCitizenDetails {
  child: ChildBasics
  content: DocumentContent
  id: UUID
  publishedAt: HelsinkiDateTime | null
  status: DocumentStatus
  template: DocumentTemplate
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentCitizenSummary
*/
export interface ChildDocumentCitizenSummary {
  id: UUID
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
  childId: UUID
  templateId: UUID
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentDetails
*/
export interface ChildDocumentDetails {
  child: ChildBasics
  content: DocumentContent
  id: UUID
  publishedAt: HelsinkiDateTime | null
  publishedContent: DocumentContent | null
  status: DocumentStatus
  template: DocumentTemplate
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentSummary
*/
export interface ChildDocumentSummary {
  id: UUID
  modifiedAt: HelsinkiDateTime
  publishedAt: HelsinkiDateTime | null
  status: DocumentStatus
  templateId: UUID
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
* Generated from fi.espoo.evaka.document.childdocument.DocumentContent
*/
export interface DocumentContent {
  answers: AnsweredQuestion[]
}

/**
* Generated from fi.espoo.evaka.document.DocumentLanguage
*/
export type DocumentLanguage =
  | 'FI'
  | 'SV'

/**
* Generated from fi.espoo.evaka.document.childdocument.DocumentStatus
*/
export type DocumentStatus =
  | 'DRAFT'
  | 'PREPARED'
  | 'COMPLETED'

/**
* Generated from fi.espoo.evaka.document.DocumentTemplate
*/
export interface DocumentTemplate {
  confidential: boolean
  content: DocumentTemplateContent
  id: UUID
  language: DocumentLanguage
  legalBasis: string
  name: string
  published: boolean
  type: DocumentType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.document.DocumentTemplateBasicsRequest
*/
export interface DocumentTemplateBasicsRequest {
  confidential: boolean
  language: DocumentLanguage
  legalBasis: string
  name: string
  type: DocumentType
  validity: DateRange
}

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
  id: UUID
  language: DocumentLanguage
  name: string
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
  'MIGRATED_LEOPS'
] as const

export type DocumentType = typeof documentTypes[number]

/**
* Generated from fi.espoo.evaka.document.ExportedDocumentTemplate
*/
export interface ExportedDocumentTemplate {
  confidential: boolean
  content: DocumentTemplateContent
  language: DocumentLanguage
  legalBasis: string
  name: string
  type: DocumentType
  validity: DateRange
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
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    template: deserializeJsonDocumentTemplate(json.template)
  }
}


export function deserializeJsonChildDocumentCitizenSummary(json: JsonOf<ChildDocumentCitizenSummary>): ChildDocumentCitizenSummary {
  return {
    ...json,
    publishedAt: HelsinkiDateTime.parseIso(json.publishedAt)
  }
}


export function deserializeJsonChildDocumentDetails(json: JsonOf<ChildDocumentDetails>): ChildDocumentDetails {
  return {
    ...json,
    child: deserializeJsonChildBasics(json.child),
    content: deserializeJsonDocumentContent(json.content),
    publishedAt: (json.publishedAt != null) ? HelsinkiDateTime.parseIso(json.publishedAt) : null,
    publishedContent: (json.publishedContent != null) ? deserializeJsonDocumentContent(json.publishedContent) : null,
    template: deserializeJsonDocumentTemplate(json.template)
  }
}


export function deserializeJsonChildDocumentSummary(json: JsonOf<ChildDocumentSummary>): ChildDocumentSummary {
  return {
    ...json,
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


export function deserializeJsonDocumentTemplate(json: JsonOf<DocumentTemplate>): DocumentTemplate {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonDocumentTemplateBasicsRequest(json: JsonOf<DocumentTemplateBasicsRequest>): DocumentTemplateBasicsRequest {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonDocumentTemplateSummary(json: JsonOf<DocumentTemplateSummary>): DocumentTemplateSummary {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}


export function deserializeJsonExportedDocumentTemplate(json: JsonOf<ExportedDocumentTemplate>): ExportedDocumentTemplate {
  return {
    ...json,
    validity: DateRange.parseJson(json.validity)
  }
}
