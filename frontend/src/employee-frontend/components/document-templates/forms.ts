// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { string } from 'lib-common/form/fields'
import { array, object, validated } from 'lib-common/form/form'
import { BoundForm, useForm } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonEmpty } from 'lib-common/form/validators'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'

import { useTranslation } from '../../state/i18n'

import {
  questionForm,
  serializeToApiTemplateQuestion,
  deserializeFromApiTemplateQuestion,
  initializeQuestionState
} from './question-descriptors/questions'

export const templateSectionForm = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty),
  questions: array(questionForm)
})

export const templateContentForm = object({
  sections: array(templateSectionForm)
})

export const serializeTemplateContentFormToDocumentTemplateContent = (
  formState: StateOf<typeof templateContentForm>
): DocumentTemplateContent => ({
  sections: formState.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map(serializeToApiTemplateQuestion)
  }))
})

export const deserializeDocumentTemplateContentToTemplateContentForm = (
  template: DocumentTemplateContent
): StateOf<typeof templateContentForm> => ({
  sections: template.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map(deserializeFromApiTemplateQuestion)
  }))
})

export const documentSectionForm = object({
  id: string(),
  label: string(),
  questions: array(questionForm)
})

export const documentForm = array(documentSectionForm)

const getDocumentInitialState = (
  template: DocumentTemplateContent
): StateOf<typeof documentForm> => {
  return template.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map((question) =>
      initializeQuestionState(question)
    )
  }))
}

export const useTemplateContentAsForm = (
  template: DocumentTemplateContent
): BoundForm<typeof documentForm> => {
  const { i18n } = useTranslation()
  return useForm(
    documentForm,
    () => getDocumentInitialState(template),
    i18n.validationErrors
  )
}

export const serializeDocumentAnswers = (
  form: BoundForm<typeof documentForm>
) => {
  return form.state
    .flatMap((section) => section.questions)
    .reduce(
      (answers, question) => ({
        ...answers,
        [question.state.id]: question.state.answer
      }),
      {}
    )
}
