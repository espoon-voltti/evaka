// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import HelsinkiDateTime from '../helsinki-date-time'
import { JsonOf } from '../json'
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

export type VasuQuestionType = (typeof vasuQuestionTypes)[number] | 'PARAGRAPH'

export interface VasuQuestionCommon {
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
  notNumbered?: boolean
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
  textValue?: Record<string, string>
  dateValue?: Record<string, LocalDate>
  dateRangeValue?: Record<string, { start: LocalDate; end: LocalDate }>
}

export interface QuestionOption {
  key: string
  name: string
  textAnswer?: boolean
  dateRange?: boolean
  isIntervention?: boolean
  info?: string
  subText?: string
  date?: boolean
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

export const mapVasuQuestion = (
  question: JsonOf<VasuQuestion>
): VasuQuestion =>
  isDateQuestionJson(question)
    ? {
        ...question,
        value: LocalDate.parseNullableIso(question.value)
      }
    : isFollowupJson(question)
      ? {
          ...question,
          value: question.value.map((entry: JsonOf<FollowupEntry>) => ({
            ...entry,
            date: LocalDate.parseIso(entry.date),
            edited: entry.edited && {
              ...entry.edited,
              editedAt: LocalDate.parseIso(entry.edited.editedAt)
            },
            createdDate:
              typeof entry.createdDate === 'string'
                ? HelsinkiDateTime.parseIso(entry.createdDate)
                : undefined
          }))
        }
      : isRadioGroupQuestionJson(question)
        ? {
            ...question,
            dateRange: question.dateRange && {
              start: LocalDate.parseIso(question.dateRange.start),
              end: LocalDate.parseIso(question.dateRange.end)
            }
          }
        : isMultiSelectQuestionJson(question)
          ? {
              ...question,
              dateValue:
                question.dateValue &&
                mapValues(question.dateValue, (v) => LocalDate.parseIso(v)),
              dateRangeValue:
                question.dateRangeValue &&
                mapValues(question.dateRangeValue, (v) => ({
                  start: LocalDate.parseIso(v.start),
                  end: LocalDate.parseIso(v.end)
                }))
            }
          : question

function isDateQuestionJson(
  question: JsonOf<VasuQuestion>
): question is JsonOf<DateQuestion> {
  return question.type === 'DATE'
}

function isFollowupJson(
  question: JsonOf<VasuQuestion>
): question is JsonOf<Followup> {
  return question.type === 'FOLLOWUP'
}

function isRadioGroupQuestionJson(
  question: JsonOf<VasuQuestion>
): question is JsonOf<RadioGroupQuestion> {
  return question.type === 'RADIO_GROUP'
}

function isMultiSelectQuestionJson(
  question: JsonOf<VasuQuestion>
): question is JsonOf<MultiSelectQuestion> {
  return question.type === 'MULTISELECT'
}

export interface Field {
  name: string
  info?: string
}
