// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { union } from 'lib-common/form/form'
import { useFormUnion } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { Question } from 'lib-common/generated/api-types/document'

import CheckboxGroupQuestionDescriptor from './CheckboxGroupQuestionDescriptor'
import CheckboxQuestionDescriptor from './CheckboxQuestionDescriptor'
import TextQuestionDescriptor from './TextQuestionDescriptor'
import { QuestionType, BoundViewProps } from './types'

// TODO: could be part of generated types?
export const questionTypes: QuestionType[] = [
  'TEXT',
  'CHECKBOX',
  'CHECKBOX_GROUP'
]

export const questionForm = union({
  TEXT: TextQuestionDescriptor.form,
  CHECKBOX: CheckboxQuestionDescriptor.form,
  CHECKBOX_GROUP: CheckboxGroupQuestionDescriptor.form
})

// TODO: Could these 7 trivial switch case functions be generated without losing type-safety?
// We would really only need to define a Record<QuestionType, QuestionDescriptor>

export const getQuestionInitialStateByType = (
  type: QuestionType
): StateOf<typeof questionForm> => {
  switch (type) {
    case 'TEXT':
      return {
        branch: type,
        state: TextQuestionDescriptor.getInitialState()
      }
    case 'CHECKBOX':
      return {
        branch: type,
        state: CheckboxQuestionDescriptor.getInitialState()
      }
    case 'CHECKBOX_GROUP':
      return {
        branch: type,
        state: CheckboxGroupQuestionDescriptor.getInitialState()
      }
  }
}

export const initializeQuestionState = (
  question: Question
): StateOf<typeof questionForm> => {
  switch (question.type) {
    case 'TEXT':
      return {
        branch: question.type,
        state: TextQuestionDescriptor.getInitialState(question)
      }
    case 'CHECKBOX':
      return {
        branch: question.type,
        state: CheckboxQuestionDescriptor.getInitialState(question)
      }
    case 'CHECKBOX_GROUP':
      return {
        branch: question.type,
        state: CheckboxGroupQuestionDescriptor.getInitialState(question)
      }
  }
}

export const DocumentQuestionView = React.memo(function DocumentQuestionView({
  bind
}: BoundViewProps<typeof questionForm>) {
  const { branch, form } = useFormUnion(bind)

  switch (branch) {
    case 'TEXT':
      return <TextQuestionDescriptor.View bind={form} />
    case 'CHECKBOX':
      return <CheckboxQuestionDescriptor.View bind={form} />
    case 'CHECKBOX_GROUP':
      return <CheckboxGroupQuestionDescriptor.View bind={form} />
  }
})

export const DocumentQuestionReadOnlyView = React.memo(
  function DocumentQuestionReadOnlyView({
    bind
  }: BoundViewProps<typeof questionForm>) {
    const { branch, form } = useFormUnion(bind)

    switch (branch) {
      case 'TEXT':
        return <TextQuestionDescriptor.ReadOnlyView bind={form} />
      case 'CHECKBOX':
        return <CheckboxQuestionDescriptor.ReadOnlyView bind={form} />
      case 'CHECKBOX_GROUP':
        return <CheckboxGroupQuestionDescriptor.ReadOnlyView bind={form} />
    }
  }
)

export const TemplateQuestionConfigView = React.memo(
  function TemplateQuestionConfigView({
    bind
  }: BoundViewProps<typeof questionForm>) {
    const { branch, form } = useFormUnion(bind)

    switch (branch) {
      case 'TEXT':
        return <TextQuestionDescriptor.TemplateView bind={form} />
      case 'CHECKBOX':
        return <CheckboxQuestionDescriptor.TemplateView bind={form} />
      case 'CHECKBOX_GROUP':
        return <CheckboxGroupQuestionDescriptor.TemplateView bind={form} />
    }
  }
)

export const serializeToApiTemplateQuestion = ({
  branch,
  state
}: StateOf<typeof questionForm>): Question => {
  switch (branch) {
    case 'TEXT':
      return TextQuestionDescriptor.serialize(state)
    case 'CHECKBOX':
      return CheckboxQuestionDescriptor.serialize(state)
    case 'CHECKBOX_GROUP':
      return CheckboxGroupQuestionDescriptor.serialize(state)
  }
}

export const deserializeFromApiTemplateQuestion = (
  q: Question
): StateOf<typeof questionForm> => {
  switch (q.type) {
    case 'TEXT':
      return {
        branch: q.type,
        state: TextQuestionDescriptor.deserialize(q)
      }
    case 'CHECKBOX':
      return {
        branch: q.type,
        state: CheckboxQuestionDescriptor.deserialize(q)
      }
    case 'CHECKBOX_GROUP':
      return {
        branch: q.type,
        state: CheckboxGroupQuestionDescriptor.deserialize(q)
      }
  }
}
