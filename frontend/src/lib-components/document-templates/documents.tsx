// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { string } from 'lib-common/form/fields'
import { array, mapped, object, union } from 'lib-common/form/form'
import { BoundForm, useFormUnion } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  AnsweredQuestion,
  DocumentContent,
  DocumentTemplateContent,
  Question
} from 'lib-common/generated/api-types/document'

import CheckboxGroupQuestionDescriptor from './question-descriptors/CheckboxGroupQuestionDescriptor'
import CheckboxQuestionDescriptor from './question-descriptors/CheckboxQuestionDescriptor'
import DateQuestionDescriptor from './question-descriptors/DateQuestionDescriptor'
import GroupedTextFieldsQuestionDescriptor from './question-descriptors/GroupedTextFieldsQuestionDescriptor'
import RadioButtonGroupQuestionDescriptor from './question-descriptors/RadioButtonGroupQuestionDescriptor'
import StaticTextDisplayQuestionDescriptor from './question-descriptors/StaticTextDisplayQuestionDescriptor'
import TextQuestionDescriptor from './question-descriptors/TextQuestionDescriptor'

export const documentQuestionForm = union({
  TEXT: TextQuestionDescriptor.document.form,
  CHECKBOX: CheckboxQuestionDescriptor.document.form,
  CHECKBOX_GROUP: CheckboxGroupQuestionDescriptor.document.form,
  RADIO_BUTTON_GROUP: RadioButtonGroupQuestionDescriptor.document.form,
  STATIC_TEXT_DISPLAY: StaticTextDisplayQuestionDescriptor.document.form,
  DATE: DateQuestionDescriptor.document.form,
  GROUPED_TEXT_FIELDS: GroupedTextFieldsQuestionDescriptor.document.form
})

export const documentSectionForm = mapped(
  object({
    id: string(),
    label: string(),
    questions: array(documentQuestionForm),
    infoText: string()
  }),
  (output): DocumentContent => ({
    answers: output.questions.map((it) => it.value)
  })
)

export const documentForm = mapped(
  array(documentSectionForm),
  (output): DocumentContent => ({
    answers: output.flatMap((section) => section.answers)
  })
)

export const DocumentQuestionView = React.memo(function DocumentQuestionView({
  bind,
  readOnly
}: {
  bind: BoundForm<typeof documentQuestionForm>
  readOnly: boolean
}) {
  const { branch, form } = useFormUnion(bind)

  switch (branch) {
    case 'TEXT':
      return (
        <TextQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
    case 'CHECKBOX':
      return (
        <CheckboxQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
    case 'CHECKBOX_GROUP':
      return (
        <CheckboxGroupQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
    case 'RADIO_BUTTON_GROUP':
      return (
        <RadioButtonGroupQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
    case 'STATIC_TEXT_DISPLAY':
      return (
        <StaticTextDisplayQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
    case 'DATE':
      return (
        <DateQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
    case 'GROUPED_TEXT_FIELDS':
      return (
        <GroupedTextFieldsQuestionDescriptor.document.Component
          bind={form}
          readOnly={readOnly}
        />
      )
  }
})

export const getDocumentQuestionInitialState = (
  question: Question,
  answeredQuestion?: AnsweredQuestion
) => {
  switch (question.type) {
    case 'TEXT':
      if (answeredQuestion?.type === question.type) {
        return TextQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return TextQuestionDescriptor.document.getInitialState(question)
    case 'CHECKBOX':
      if (answeredQuestion?.type === question.type) {
        return CheckboxQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return CheckboxQuestionDescriptor.document.getInitialState(question)
    case 'CHECKBOX_GROUP':
      if (answeredQuestion?.type === question.type) {
        return CheckboxGroupQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return CheckboxGroupQuestionDescriptor.document.getInitialState(question)
    case 'RADIO_BUTTON_GROUP':
      if (answeredQuestion?.type === question.type) {
        return RadioButtonGroupQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return RadioButtonGroupQuestionDescriptor.document.getInitialState(
        question
      )
    case 'STATIC_TEXT_DISPLAY':
      if (answeredQuestion?.type === question.type) {
        return StaticTextDisplayQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return StaticTextDisplayQuestionDescriptor.document.getInitialState(
        question
      )
    case 'DATE':
      if (answeredQuestion?.type === question.type) {
        return DateQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return DateQuestionDescriptor.document.getInitialState(question)
    case 'GROUPED_TEXT_FIELDS':
      if (answeredQuestion?.type === question.type) {
        return GroupedTextFieldsQuestionDescriptor.document.getInitialState(
          question,
          answeredQuestion.answer
        )
      }
      return GroupedTextFieldsQuestionDescriptor.document.getInitialState(
        question
      )
  }
}

export const getDocumentFormInitialState = (
  templateContent: DocumentTemplateContent,
  content?: DocumentContent
): StateOf<typeof documentForm> =>
  templateContent.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map((question) => {
      const answer = content?.answers?.find((a) => a.questionId === question.id)
      return getDocumentQuestionInitialState(question, answer)
    }),
    infoText: section.infoText
  }))
