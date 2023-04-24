// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import DateRange from '../../date-range'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.document.DocumentTemplate
*/
export interface DocumentTemplate {
  content: DocumentTemplateContent
  id: UUID
  name: string
  published: boolean
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
  validity: DateRange
}

/**
* Generated from fi.espoo.evaka.document.DocumentTemplateSummary
*/
export interface DocumentTemplateSummary {
  id: UUID
  name: string
  published: boolean
  validity: DateRange
}

export namespace Question {
  /**
  * Generated from fi.espoo.evaka.document.Question.MultiselectQuestion
  */
  export interface MultiselectQuestion {
    type: 'MULTISELECT'
    id: string
    label: string
    options: string[]
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
export type Question = Question.MultiselectQuestion | Question.TextQuestion


/**
* Generated from fi.espoo.evaka.document.Section
*/
export interface Section {
  id: string
  label: string
  questions: Question[]
}
