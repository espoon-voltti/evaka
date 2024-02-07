// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CheckboxQuestion,
  DateQuestion,
  Followup,
  mapVasuQuestion,
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
import { JsonOf } from 'lib-common/json'

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

export const mapVasuContent = (content: JsonOf<VasuContent>): VasuContent => ({
  ...content,
  sections: content.sections.map((section: JsonOf<VasuSection>) => ({
    ...section,
    questions: section.questions.map(mapVasuQuestion)
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
