// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ComponentType } from 'react'

import { BoundForm } from 'lib-common/form/hooks'
import { AnyForm, StateOf } from 'lib-common/form/types'
import { Question } from 'lib-common/generated/api-types/document'

export type QuestionType = Question['type']

export const questionTypes: QuestionType[] = [
  'TEXT',
  'CHECKBOX',
  'CHECKBOX_GROUP'
]

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
