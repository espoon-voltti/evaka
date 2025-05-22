// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { BoundForm } from 'lib-common/form/hooks'
import { useFormUnion } from 'lib-common/form/hooks'
import CheckboxGroupQuestionDescriptor from 'lib-components/document-templates/question-descriptors/CheckboxGroupQuestionDescriptor'
import CheckboxQuestionDescriptor from 'lib-components/document-templates/question-descriptors/CheckboxQuestionDescriptor'
import DateQuestionDescriptor from 'lib-components/document-templates/question-descriptors/DateQuestionDescriptor'
import GroupedTextFieldsQuestionDescriptor from 'lib-components/document-templates/question-descriptors/GroupedTextFieldsQuestionDescriptor'
import RadioButtonGroupQuestionDescriptor from 'lib-components/document-templates/question-descriptors/RadioButtonGroupQuestionDescriptor'
import StaticTextDisplayQuestionDescriptor from 'lib-components/document-templates/question-descriptors/StaticTextDisplayQuestionDescriptor'
import TextQuestionDescriptor from 'lib-components/document-templates/question-descriptors/TextQuestionDescriptor'

import type { templateQuestionForm } from './forms'

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
      case 'DATE':
        return <DateQuestionDescriptor.template.Component bind={form} />
      case 'GROUPED_TEXT_FIELDS':
        return (
          <GroupedTextFieldsQuestionDescriptor.template.Component bind={form} />
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
      case 'DATE':
        return <DateQuestionDescriptor.template.PreviewComponent bind={form} />
      case 'GROUPED_TEXT_FIELDS':
        return (
          <GroupedTextFieldsQuestionDescriptor.template.PreviewComponent
            bind={form}
          />
        )
    }
  }
)
