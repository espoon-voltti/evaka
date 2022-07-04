// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import LocalDate from '../local-date'

export const vasuQuestionTypes = [
  'TEXT',
  'CHECKBOX',
  'RADIO_GROUP',
  'MULTISELECT',
  'MULTI_FIELD',
  'MULTI_FIELD_LIST',
  'DATE',
  'FOLLOWUP',
  'STATIC_INFO_SUBSECTION'
] as const

export type VasuQuestionType = typeof vasuQuestionTypes[number] | 'PARAGRAPH'

interface VasuQuestionCommon {
  type: VasuQuestionType
  name: string
  ophKey: string | null
  info: string
  id: string | null
  dependsOn: string[] | null
}

export interface TextQuestion extends VasuQuestionCommon {
  type: 'TEXT'
  multiline: boolean
  value: string
}

export interface CheckboxQuestion extends VasuQuestionCommon {
  type: 'CHECKBOX'
  value: boolean
  label: string | null
}

export interface RadioGroupQuestion extends VasuQuestionCommon {
  type: 'RADIO_GROUP'
  options: QuestionOption[]
  value: string | null
  dateRange: {
    start: LocalDate
    end: LocalDate
  } | null
}

export interface MultiSelectQuestion extends VasuQuestionCommon {
  type: 'MULTISELECT'
  options: QuestionOption[]
  minSelections: number
  maxSelections: number | null
  value: string[]
  textValue?: TextValueMap
}

export interface QuestionOption {
  key: string
  name: string
  textAnswer?: boolean
  dateRange?: boolean
  isIntervention?: boolean
  info?: string
}

export interface TextValueMap {
  [key: string]: string
}

export interface MultiFieldQuestion extends VasuQuestionCommon {
  type: 'MULTI_FIELD'
  keys: Field[]
  value: string[]
  separateRows: boolean
}

export interface MultiFieldListQuestion extends VasuQuestionCommon {
  type: 'MULTI_FIELD_LIST'
  keys: Field[]
  value: string[][]
}

export interface DateQuestion extends VasuQuestionCommon {
  type: 'DATE'
  trackedInEvents: boolean
  nameInEvents: string
  value: LocalDate | null
}

export interface Followup extends VasuQuestionCommon {
  type: 'FOLLOWUP'
  title: string
  value: FollowupEntry[]
  continuesNumbering: boolean
}

export interface FollowupEntry {
  date: LocalDate
  authorName: string
  text: string
  id?: string
  authorId?: string
  edited?: FollowupEntryEditDetails
  createdDate?: HelsinkiDateTime
}

export interface FollowupEntryEditDetails {
  editedAt: LocalDate
  editorName: string
  editorId?: string
}

export interface Paragraph extends VasuQuestionCommon {
  type: 'PARAGRAPH'
  title: string
  paragraph: string
}

export interface StaticInfoSubsection extends VasuQuestionCommon {
  type: 'STATIC_INFO_SUBSECTION'
}

export type VasuQuestion =
  | TextQuestion
  | CheckboxQuestion
  | RadioGroupQuestion
  | MultiSelectQuestion
  | MultiFieldQuestion
  | MultiFieldListQuestion
  | DateQuestion
  | Followup
  | Paragraph
  | StaticInfoSubsection

interface Field {
  name: string
  info?: string
}
