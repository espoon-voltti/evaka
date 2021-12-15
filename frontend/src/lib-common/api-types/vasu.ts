// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '../local-date'
import { VasuDocument } from 'lib-common/generated/api-types/vasu'

export const vasuQuestionTypes = [
  'TEXT',
  'CHECKBOX',
  'RADIO_GROUP',
  'MULTISELECT',
  'FOLLOWUP'
] as const

export type VasuQuestionType = typeof vasuQuestionTypes[number]

interface VasuQuestionCommon {
  type: VasuQuestionType
  name: string
  ophKey: string | null
  info: string
}

export interface TextQuestion extends VasuQuestionCommon {
  multiline: boolean
  value: string
}

export interface CheckboxQuestion extends VasuQuestionCommon {
  value: boolean
}

export interface RadioGroupQuestion extends VasuQuestionCommon {
  options: QuestionOption[]
  value: string | null
}

export interface MultiSelectQuestion extends VasuQuestionCommon {
  options: QuestionOption[]
  minSelections: number
  maxSelections: number | null
  value: string[]
}

export interface QuestionOption {
  key: string
  name: string
}

export interface Followup extends VasuQuestionCommon {
  title: string
  value: FollowupEntry[]
}

export interface FollowupEntry {
  date: LocalDate
  authorName: string
  text: string
  id?: string
  authorId?: string
  edited?: FollowupEntryEditDetails
}

export interface FollowupEntryEditDetails {
  editedAt: LocalDate
  editorName: string
  editorId?: string
}

export type VasuQuestion =
  | TextQuestion
  | CheckboxQuestion
  | RadioGroupQuestion
  | MultiSelectQuestion
  | Followup

export type PermittedFollowupActions = {
  [key: string]: string[]
}

export interface GetVasuDocumentResponse {
  permittedFollowupActions: PermittedFollowupActions
  vasu: VasuDocument
}
