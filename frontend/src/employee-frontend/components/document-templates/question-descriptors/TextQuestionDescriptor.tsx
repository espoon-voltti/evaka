// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect } from 'react'
import { v4 as uuidv4 } from 'uuid'

import { string } from 'lib-common/form/fields'
import { mapped, object, validated, value } from 'lib-common/form/form'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonEmpty } from 'lib-common/form/validators'
import {
  AnsweredQuestion,
  Question
} from 'lib-common/generated/api-types/document'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'

import {
  DocumentQuestionDescriptor,
  QuestionType,
  TemplateQuestionDescriptor
} from './types'

const questionType: QuestionType = 'TEXT'

type ApiQuestion = Question.TextQuestion

const templateForm = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty),
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

type Answer = string

const getAnswerInitialValue = (): Answer => ''

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

const View = React.memo(function View({
  bind,
  readOnly
}: {
  bind: BoundForm<QuestionForm>
  readOnly: boolean
}) {
  const { i18n } = useTranslation()
  const { template, answer } = useFormFields(bind)
  const { label, infoText } = useFormFields(template)
  return readOnly ? (
    <FixedSpaceColumn>
      <Label>{label.state}</Label>
      <span>{answer.state}</span>
    </FixedSpaceColumn>
  ) : (
    <FixedSpaceColumn fullWidth>
      <ExpandingInfo
        info={infoText.value()}
        width="full"
        ariaLabel=""
        closeLabel={i18n.common.close}
      >
        <Label>{label.state}</Label>
      </ExpandingInfo>
      <InputFieldF bind={answer} readonly={false} width="L" />
    </FixedSpaceColumn>
  )
})

const Preview = React.memo(function Preview({
  bind
}: {
  bind: BoundForm<TemplateForm>
}) {
  const { i18n } = useTranslation()

  const getInitialPreviewState = useCallback(
    () => ({
      template: bind.state,
      answer: getAnswerInitialValue()
    }),
    [bind.state]
  )

  const mockBind = useForm(
    questionForm,
    getInitialPreviewState,
    i18n.validationErrors
  )

  const { set } = mockBind
  useEffect(() => {
    set(getInitialPreviewState())
  }, [getInitialPreviewState, set])

  return <View bind={mockBind} readOnly={false} />
})

const TemplateView = React.memo(function TemplateView({
  bind
}: {
  bind: BoundForm<TemplateForm>
}) {
  const { i18n } = useTranslation()
  const { label, infoText } = useFormFields(bind)

  return (
    <FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.label}</Label>
        <InputFieldF bind={label} hideErrorsBeforeTouched />
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.infoText}</Label>
        <InputFieldF bind={infoText} hideErrorsBeforeTouched />
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
      answer: answer ?? getAnswerInitialValue()
    }
  }),
  Component: View
}

export default {
  template: templateQuestionDescriptor,
  document: documentQuestionDescriptor
}
