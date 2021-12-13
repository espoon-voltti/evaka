// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CheckboxQuestion,
  Followup,
  FollowupEntry,
  MultiFieldListQuestion,
  MultiFieldQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion,
  VasuQuestion
} from 'lib-common/api-types/vasu'
import { VasuContent, VasuSection } from 'lib-common/generated/api-types/vasu'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

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

export function isMultiFieldQuestion(
  question: VasuQuestion
): question is MultiFieldQuestion {
  return question.type === 'MULTI_FIELD'
}

export function isMultiFieldListQuestion(
  question: VasuQuestion
): question is MultiFieldListQuestion {
  return question.type === 'MULTI_FIELD_LIST'
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
