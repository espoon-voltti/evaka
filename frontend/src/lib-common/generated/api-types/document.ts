// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import DateRange from '../../date-range'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.document.childdocument.AnsweredQuestion
*/
export interface AnsweredQuestion {
  answer: unknown
  questionId: string
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocument
*/
export interface ChildDocument {
  childId: UUID
  content: DocumentContent
  id: UUID
  published: boolean
  template: DocumentTemplate
}

/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentCreateRequest
*/
export interface ChildDocumentCreateRequest {
  childId: UUID
  templateId: UUID
}

/**
* Generated from fi.espoo.evaka.document.childdocument.DocumentContent
*/
export interface DocumentContent {
  answers: AnsweredQuestion[]
}

/**
* Generated from fi.espoo.evaka.document.DocumentTemplate
*/
export interface DocumentTemplate {
  content: DocumentTemplateContent
  id: UUID
  name: string
  published: boolean
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
* Generated from fi.espoo.evaka.document.DocumentTemplateCreateRequest
*/
export interface DocumentTemplateCreateRequest {
  name: string
  type: DocumentType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.document.DocumentTemplateSummary
*/
export interface DocumentTemplateSummary {
  id: UUID
  name: string
  published: boolean
  type: DocumentType
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.document.DocumentType
*/
export type DocumentType =
  | 'PEDAGOGICAL_REPORT'
  | 'PEDAGOGICAL_ASSESSMENT'

/**
* Generated from fi.espoo.evaka.document.MultiselectOption
*/
export interface MultiselectOption {
  id: string
  label: string
}

export namespace Question {
  /**
  * Generated from fi.espoo.evaka.document.Question.CheckboxGroupQuestion
  */
  export interface CheckboxGroupQuestion {
    type: 'CHECKBOX_GROUP'
    id: string
    label: string
    options: MultiselectOption[]
  }
  
  /**
  * Generated from fi.espoo.evaka.document.Question.CheckboxQuestion
  */
  export interface CheckboxQuestion {
    type: 'CHECKBOX'
    id: string
    label: string
  }
  
  /**
  * Generated from fi.espoo.evaka.document.Question.TextQuestion
  */
  export interface TextQuestion {
    type: 'TEXT'
    id: string
    label: string
  }
}

/**
* Generated from fi.espoo.evaka.document.Question
*/
export type Question = Question.CheckboxGroupQuestion | Question.CheckboxQuestion | Question.TextQuestion


/**
* Generated from fi.espoo.evaka.document.Section
*/
export interface Section {
  id: string
  label: string
  questions: Question[]
}
