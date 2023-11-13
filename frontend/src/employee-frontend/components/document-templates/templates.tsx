// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { string } from 'lib-common/form/fields'
import { array, mapped, object, union, validated } from 'lib-common/form/form'
import { BoundForm, useFormUnion } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import {
  DocumentTemplateContent,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import CheckboxGroupQuestionDescriptor from 'lib-components/document-templates/question-descriptors/CheckboxGroupQuestionDescriptor'
import CheckboxQuestionDescriptor from 'lib-components/document-templates/question-descriptors/CheckboxQuestionDescriptor'
import RadioButtonGroupQuestionDescriptor from 'lib-components/document-templates/question-descriptors/RadioButtonGroupQuestionDescriptor'
import StaticTextDisplayQuestionDescriptor from 'lib-components/document-templates/question-descriptors/StaticTextDisplayQuestionDescriptor'
import TextQuestionDescriptor from 'lib-components/document-templates/question-descriptors/TextQuestionDescriptor'

export const templateQuestionForm = mapped(
  union({
    TEXT: TextQuestionDescriptor.template.form,
    CHECKBOX: CheckboxQuestionDescriptor.template.form,
    CHECKBOX_GROUP: CheckboxGroupQuestionDescriptor.template.form,
    RADIO_BUTTON_GROUP: RadioButtonGroupQuestionDescriptor.template.form,
    STATIC_TEXT_DISPLAY: StaticTextDisplayQuestionDescriptor.template.form
  }),
  (output): Question => {
    switch (output.branch) {
      case 'TEXT':
        return {
          type: output.branch,
          ...output.value
        }
      case 'CHECKBOX':
        return {
          type: output.branch,
          ...output.value
        }
      case 'CHECKBOX_GROUP':
        return {
          type: output.branch,
          ...output.value
        }
      case 'RADIO_BUTTON_GROUP':
        return {
          type: output.branch,
          ...output.value
        }
      case 'STATIC_TEXT_DISPLAY':
        return {
          type: output.branch,
          ...output.value
        }
    }
  }
)

export const templateSectionForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank),
  questions: array(templateQuestionForm),
  infoText: string()
})

export const templateContentForm = object({
  sections: array(templateSectionForm)
})

export const TemplateQuestionConfigView = React.memo(
  function TemplateQuestionConfigView({
    bind
  }: {
    bind: BoundForm<typeof templateQuestionForm>
  }) {
    const { branch, form } = useFormUnion(bind)

    switch (branch) {
      case 'TEXT':
        return <TextQuestionDescriptor.template.Component bind={form} />
      case 'CHECKBOX':
        return <CheckboxQuestionDescriptor.template.Component bind={form} />
      case 'CHECKBOX_GROUP':
        return (
          <CheckboxGroupQuestionDescriptor.template.Component bind={form} />
        )
      case 'RADIO_BUTTON_GROUP':
        return (
          <RadioButtonGroupQuestionDescriptor.template.Component bind={form} />
        )
      case 'STATIC_TEXT_DISPLAY':
        return (
          <StaticTextDisplayQuestionDescriptor.template.Component bind={form} />
        )
    }
  }
)

export const TemplateQuestionPreview = React.memo(
  function TemplateQuestionPreview({
    bind
  }: {
    bind: BoundForm<typeof templateQuestionForm>
  }) {
    const { branch, form } = useFormUnion(bind)

    switch (branch) {
      case 'TEXT':
        return <TextQuestionDescriptor.template.PreviewComponent bind={form} />
      case 'CHECKBOX':
        return (
          <CheckboxQuestionDescriptor.template.PreviewComponent bind={form} />
        )
      case 'CHECKBOX_GROUP':
        return (
          <CheckboxGroupQuestionDescriptor.template.PreviewComponent
            bind={form}
          />
        )
      case 'RADIO_BUTTON_GROUP':
        return (
          <RadioButtonGroupQuestionDescriptor.template.PreviewComponent
            bind={form}
          />
        )
      case 'STATIC_TEXT_DISPLAY':
        return (
          <StaticTextDisplayQuestionDescriptor.template.PreviewComponent
            bind={form}
          />
        )
    }
  }
)

export const getTemplateQuestionInitialState = (question: Question) => {
  switch (question.type) {
    case 'TEXT':
      return TextQuestionDescriptor.template.getInitialState(question)
    case 'CHECKBOX':
      return CheckboxQuestionDescriptor.template.getInitialState(question)
    case 'CHECKBOX_GROUP':
      return CheckboxGroupQuestionDescriptor.template.getInitialState(question)
    case 'RADIO_BUTTON_GROUP':
      return RadioButtonGroupQuestionDescriptor.template.getInitialState(
        question
      )
    case 'STATIC_TEXT_DISPLAY':
      return StaticTextDisplayQuestionDescriptor.template.getInitialState(
        question
      )
  }
}

export const getTemplateQuestionInitialStateByType = (type: QuestionType) => {
  switch (type) {
    case 'TEXT':
      return TextQuestionDescriptor.template.getInitialState()
    case 'CHECKBOX':
      return CheckboxQuestionDescriptor.template.getInitialState()
    case 'CHECKBOX_GROUP':
      return CheckboxGroupQuestionDescriptor.template.getInitialState()
    case 'RADIO_BUTTON_GROUP':
      return RadioButtonGroupQuestionDescriptor.template.getInitialState()
    case 'STATIC_TEXT_DISPLAY':
      return StaticTextDisplayQuestionDescriptor.template.getInitialState()
  }
}

export const getTemplateFormInitialState = (
  template: DocumentTemplateContent
): StateOf<typeof templateContentForm> => ({
  sections: template.sections.map((section) => ({
    id: section.id,
    label: section.label,
    questions: section.questions.map(getTemplateQuestionInitialState),
    infoText: section.infoText
  }))
})
