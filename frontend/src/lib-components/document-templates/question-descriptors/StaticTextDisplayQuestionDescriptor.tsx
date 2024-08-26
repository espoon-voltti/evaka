// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'
import styled from 'styled-components'
import { v4 as uuidv4 } from 'uuid'

import { string } from 'lib-common/form/fields'
import { mapped, object, validated, value } from 'lib-common/form/form'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import {
  AnsweredQuestion,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'

import { useTranslations } from '../../i18n'
import { defaultMargins } from '../../white-space'

import { DocumentQuestionDescriptor, TemplateQuestionDescriptor } from './types'

const questionType: QuestionType = 'STATIC_TEXT_DISPLAY'

type ApiQuestion = Question.StaticTextDisplayQuestion

const templateForm = object({
  id: validated(string(), nonBlank),
  label: string(),
  text: string(),
  infoText: string()
})

type TemplateForm = typeof templateForm

const getTemplateInitialValues = (
  question?: ApiQuestion
): StateOf<TemplateForm> => ({
  id: question?.id ?? uuidv4(),
  label: question?.label ?? '',
  text: question?.text ?? '',
  infoText: question?.infoText ?? ''
})

type Answer = null

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

const getAnswerState = (
  answer?: Answer | undefined
): StateOf<QuestionForm>['answer'] => (answer !== undefined ? answer : null)

const MoreVerticalMargin = styled.div`
  margin-top: ${defaultMargins.m};
  margin-bottom: ${defaultMargins.L};
  width: 100%;
`

const View = React.memo(function View({
  bind,
  readOnly
}: {
  bind: BoundForm<QuestionForm>
  readOnly: boolean
}) {
  const { template } = useFormFields(bind)
  const { label, text, infoText } = useFormFields(template)
  return (
    <MoreVerticalMargin>
      <FixedSpaceColumn fullWidth>
        {!!label.state && (
          <ExpandingInfo
            info={readOnly ? undefined : infoText.value()}
            width="full"
          >
            <Label>{label.state}</Label>
          </ExpandingInfo>
        )}
        {!!text.state && (
          <ExpandingInfo
            info={readOnly || label.state ? undefined : infoText.value()}
            width="full"
          >
            <div>
              {text.state.split('\n').map((line, i) => (
                <Fragment key={i}>
                  {line}
                  <br />
                </Fragment>
              ))}
            </div>
          </ExpandingInfo>
        )}
      </FixedSpaceColumn>
    </MoreVerticalMargin>
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
  const { label, text, infoText } = useFormFields(bind)

  return (
    <FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.label}</Label>
        <InputFieldF
          bind={label}
          hideErrorsBeforeTouched
          data-qa="question-title-input"
        />
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.text}</Label>
        <TextAreaF
          bind={text}
          hideErrorsBeforeTouched
          data-qa="question-text-input"
        />
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
