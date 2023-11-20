// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import {
  CheckboxQuestion,
  DateQuestion,
  Followup,
  FollowupEntry,
  MultiFieldListQuestion,
  MultiFieldQuestion,
  MultiSelectQuestion,
  Paragraph,
  RadioGroupQuestion,
  StaticInfoSubsection,
  TextQuestion,
  VasuQuestion
} from 'lib-common/api-types/vasu'
import { VasuContent, VasuSection } from 'lib-common/generated/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
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

export function isDateQuestion(
  question: VasuQuestion
): question is DateQuestion {
  return question.type === 'DATE'
}

export function isFollowup(question: VasuQuestion): question is Followup {
  return question.type === 'FOLLOWUP'
}

export function isParagraph(question: VasuQuestion): question is Paragraph {
  return question.type === 'PARAGRAPH'
}

export function isStaticInfoSubsection(
  question: VasuQuestion
): question is StaticInfoSubsection {
  return question.type === 'STATIC_INFO_SUBSECTION'
}

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

export const mapVasuContent = (content: JsonOf<VasuContent>): VasuContent => ({
  ...content,
  sections: content.sections.map((section: JsonOf<VasuSection>) => ({
    ...section,
    questions: section.questions.map((question: JsonOf<VasuQuestion>) =>
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
    )
  }))
})

export function getQuestionNumber(
  sectionIndex: number,
  sectionQuestions: VasuQuestion[],
  question: VasuQuestion
) {
  let questionIndex = 0
  for (const q of sectionQuestions) {
    if (q === question) {
      break
    }

    if (
      !isParagraph(q) &&
      !isFollowup(q) &&
      !isStaticInfoSubsection(q) &&
      (!isCheckboxQuestion(q) || !q.notNumbered)
    ) {
      questionIndex += 1
    }
  }

  return `${sectionIndex + 1}.${questionIndex + 1}`
}
