// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { v4 as uuidv4 } from 'uuid'

import { string } from 'lib-common/form/fields'
import { mapped, object, validated, value } from 'lib-common/form/form'
import type { BoundForm } from 'lib-common/form/hooks'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { StateOf } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import type {
  AnsweredQuestion,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'

import { TextAreaF } from '../../atoms/form/TextArea'
import { useTranslations } from '../../i18n'

import type {
  DocumentQuestionDescriptor,
  TemplateQuestionDescriptor
} from './types'

const questionType: QuestionType = 'CHECKBOX'

type ApiQuestion = Question.CheckboxQuestion

const templateForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank),
  infoText: string()
})

type TemplateForm = typeof templateForm

const getTemplateInitialValues = (
  question?: ApiQuestion
): StateOf<TemplateForm> => ({
  id: question?.id ?? uuidv4(),
  label: question?.label ?? '',
  infoText: question?.infoText ?? ''
})

type Answer = boolean

const questionForm = mapped(
  object({
    template: templateForm,
    answer: value<Answer>()
  }),
  (output): AnsweredQuestion => ({
    questionId: output.template.id,
    answer: output.answer,
    type: questionType
  })
)

type QuestionForm = typeof questionForm

const getAnswerState = (answer?: Answer): StateOf<QuestionForm>['answer'] =>
  answer !== undefined ? answer : false

const View = React.memo(function View({
  bind,
  readOnly
}: {
  bind: BoundForm<QuestionForm>
  readOnly: boolean
}) {
  const i18n = useTranslations()
  const { template, answer } = useFormFields(bind)
  const { label, infoText } = useFormFields(template)
  return readOnly ? (
    <FixedSpaceColumn spacing="xs">
      <Label>{label.state}</Label>
      <span>{answer.state ? i18n.common.yes : i18n.common.no}</span>
    </FixedSpaceColumn>
  ) : (
    <ExpandingInfo info={infoText.value()} width="full">
      <CheckboxF bind={answer} label={label.state} disabled={readOnly} />
    </ExpandingInfo>
  )
})

const Preview = React.memo(function Preview({
  bind
}: {
  bind: BoundForm<TemplateForm>
}) {
  const i18n = useTranslations()

  const [prevBindState, setPrevBindState] = useState(bind.state)

  const getInitialPreviewState = () => ({
    template: bind.state,
    answer: getAnswerState()
  })

  const mockBind = useForm(
    questionForm,
    getInitialPreviewState,
    i18n.validationErrors
  )

  if (bind.state !== prevBindState) {
    mockBind.set(getInitialPreviewState())
    setPrevBindState(bind.state)
  }

  return <View bind={mockBind} readOnly={false} />
})

const TemplateView = React.memo(function TemplateView({
  bind
}: {
  bind: BoundForm<TemplateForm>
}) {
  const i18n = useTranslations()
  const { label, infoText } = useFormFields(bind)

  return (
    <FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.label}</Label>
        <InputFieldF bind={label} hideErrorsBeforeTouched />
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.infoText}</Label>
        <TextAreaF bind={infoText} hideErrorsBeforeTouched />
      </FixedSpaceColumn>
    </FixedSpaceColumn>
  )
})

const templateQuestionDescriptor: TemplateQuestionDescriptor<
  typeof questionType,
  typeof templateForm,
  ApiQuestion
> = {
  form: templateForm,
  getInitialState: (question?: ApiQuestion) => ({
    branch: questionType,
    state: getTemplateInitialValues(question)
  }),
  Component: TemplateView,
  PreviewComponent: Preview
}

const documentQuestionDescriptor: DocumentQuestionDescriptor<
  typeof questionType,
  typeof questionForm,
  ApiQuestion,
  Answer
> = {
  form: questionForm,
  getInitialState: (question: ApiQuestion, answer?: Answer) => ({
    branch: questionType,
    state: {
      template: getTemplateInitialValues(question),
      answer: getAnswerState(answer)
    }
  }),
  Component: View
}

export default {
  template: templateQuestionDescriptor,
  document: documentQuestionDescriptor
}
