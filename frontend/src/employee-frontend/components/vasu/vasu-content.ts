// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export interface VasuContent {
  sections: VasuSection[]
}

export interface VasuSection {
  name: string
  questions: VasuQuestion[]
}

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
  editorId: string
}

export type VasuQuestion =
  | TextQuestion
  | CheckboxQuestion
  | RadioGroupQuestion
  | MultiSelectQuestion
  | Followup

export function isTextQuestion(
  question: VasuQuestion
): question is TextQuestion {
  return question.type === 'TEXT'
}

export function isCheckboxQuestion(
  question: VasuQuestion
): question is CheckboxQuestion {
  return question.type === 'CHECKBOX'
}

export function isRadioGroupQuestion(
  question: VasuQuestion
): question is RadioGroupQuestion {
  return question.type === 'RADIO_GROUP'
}

export function isMultiSelectQuestion(
  question: VasuQuestion
): question is MultiSelectQuestion {
  return question.type === 'MULTISELECT'
}

export function isFollowup(question: VasuQuestion): question is Followup {
  return question.type === 'FOLLOWUP'
}

function isFollowupJson(
  question: JsonOf<VasuQuestion>
): question is JsonOf<Followup> {
  return question.type === 'FOLLOWUP'
}

export const mapVasuContent = (content: JsonOf<VasuContent>): VasuContent => ({
  sections: content.sections.map((section: JsonOf<VasuSection>) => ({
    ...section,
    questions: section.questions.map((question: JsonOf<VasuQuestion>) =>
      isFollowupJson(question)
        ? {
            ...question,
            value: question.value.map((entry: JsonOf<FollowupEntry>) => ({
              ...entry,
              date: LocalDate.parseIso(entry.date),
              edited: entry.edited && {
                ...entry.edited,
                editedAt: LocalDate.parseIso(entry.edited.editedAt)
              }
            }))
          }
        : question
    )
  }))
})
