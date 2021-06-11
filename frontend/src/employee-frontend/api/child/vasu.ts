// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'employee-frontend/types'
import { Result, Success, Failure } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { JsonOf } from 'lib-common/json'
import { client } from '../client'

interface VasuTemplateSummary {
  id: UUID
  name: string
  valid: DateRange
}

export async function getVasuTemplates(): Promise<Result<VasuTemplateSummary>> {
  return client
    .get<JsonOf<VasuTemplateSummary>>(`/vasu/templates`)
    .then((res) =>
      Success.of({
        ...res.data,
        valid: DateRange.parseJson(res.data.valid)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVasuDocument(
  id: UUID
): Promise<Result<VasuDocumentResponse>> {
  return client
    .get<JsonOf<VasuDocumentResponse>>(`/vasu/${id}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function createVasuDocument(
  childId: UUID,
  templateId: UUID
): Promise<Result<null>> {
  childId = '7430ca76-ca76-11eb-a754-47fa65662af6'
  templateId = '6fce7cbc-ca76-11eb-a754-2bd0c56972db'
  return client
    .post(`/vasu`, { childId, templateId })
    .then((res) => {
      console.log('createRes: ', res)
      return Success.of(null)
    })
    .catch((e) => Failure.fromError(e))
}

export async function putVasuDocument(
  documentId: UUID,
  content: VasuContent
): Promise<Result<null>> {
  return client
    .put(`/vasu/${documentId}`, { content })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export interface VasuDocumentResponse {
  id: UUID
  child: VasuDocumentResponseChild
  templateName: string
  content: VasuContent
}

interface VasuDocumentResponseChild {
  id: UUID
  firstName: string
  lastName: string
}

interface VasuContent {
  sections: VasuSection[]
}

interface VasuSection {
  name: string
  questions: VasuQuestion[]
}

type VasuQuestionType = 'TEXT' | 'CHECKBOX' | 'RADIO_GROUP' | 'MULTISELECT'
interface VasuQuestionCommon {
  type: VasuQuestionType
  name: string
}

export interface TextQuestion extends VasuQuestionCommon {
  multiline: boolean
  value: string
}

export interface CheckboxQuestion extends VasuQuestionCommon {
  value: boolean
}

export interface RadioGroupQuestion extends VasuQuestionCommon {
  optionNames: string[]
  value: number | null
}

export interface MultiSelectQuestion extends VasuQuestionCommon {
  optionNames: string[]
  minSelections: number
  maxSelections: number | null
  value: number[]
}

type VasuQuestion =
  | TextQuestion
  | CheckboxQuestion
  | RadioGroupQuestion
  | MultiSelectQuestion

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
