// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ComponentType } from 'react'

import type { BoundForm } from 'lib-common/form/hooks'
import type { AnyForm, StateOf } from 'lib-common/form/types'
import type {
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'

export interface TemplateQuestionDescriptor<
  Key extends QuestionType,
  TemplateForm extends AnyForm,
  ApiQuestion extends Question
> {
  form: TemplateForm
  getInitialState: (question?: ApiQuestion) => {
    branch: Key
    state: StateOf<TemplateForm>
  }
  Component: ComponentType<{ bind: BoundForm<TemplateForm> }>
  PreviewComponent: ComponentType<{ bind: BoundForm<TemplateForm> }>
}

export interface DocumentQuestionDescriptor<
  Key extends QuestionType,
  QuestionForm extends AnyForm,
  ApiQuestion extends Question,
  Answer
> {
  form: QuestionForm
  getInitialState: (
    question: ApiQuestion,
    answer?: Answer
  ) => {
    branch: Key
    state: StateOf<QuestionForm>
  }
  Component: ComponentType<{
    bind: BoundForm<QuestionForm>
    readOnly: boolean
  }>
}
