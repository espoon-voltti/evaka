// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { string } from 'lib-common/form/fields'
import { object, validated, value } from 'lib-common/form/form'
import { useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonEmpty } from 'lib-common/form/validators'
import { Question } from 'lib-common/generated/api-types/document'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'

import { QuestionDescriptor, QuestionType, BoundViewProps } from './types'

const questionType: QuestionType = 'CHECKBOX'

type ApiQuestion = Question.CheckboxQuestion

type AnswerType = boolean

const getAnswerInitialValue = (): AnswerType => false

const form = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty),
  answer: value<AnswerType>()
})

const getInitialState = (question?: ApiQuestion): StateOf<typeof form> => ({
  id: question?.id ?? crypto.randomUUID(),
  label: question?.label ?? '',
  answer: getAnswerInitialValue()
})

const View = React.memo(function View({ bind }: BoundViewProps<typeof form>) {
  const { label, answer } = useFormFields(bind)
  return <CheckboxF bind={answer} label={label.state} disabled={false} />
})

const ReadOnlyView = React.memo(function ReadOnlyView({
  bind
}: BoundViewProps<typeof form>) {
  const { label, answer } = useFormFields(bind)
  return <CheckboxF bind={answer} label={label.state} disabled={true} />
})

const TemplateView = React.memo(function TemplateView({
  bind
}: BoundViewProps<typeof form>) {
  const { label } = useFormFields(bind)

  return (
    <FixedSpaceColumn>
      <Label>Otsikko</Label>
      <InputFieldF bind={label} hideErrorsBeforeTouched />
    </FixedSpaceColumn>
  )
})

const questionDescriptor: QuestionDescriptor<typeof form, ApiQuestion> = {
  form,
  getInitialState,
  View,
  ReadOnlyView,
  TemplateView,
  serialize: ({ answer: _, ...rest }: StateOf<typeof form>): ApiQuestion => ({
    type: questionType,
    ...rest
  }),
  deserialize: (q: ApiQuestion): StateOf<typeof form> => ({
    ...q,
    answer: getAnswerInitialValue()
  })
}

export default questionDescriptor
