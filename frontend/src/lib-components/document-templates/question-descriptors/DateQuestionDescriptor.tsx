// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { v4 as uuidv4 } from 'uuid'

import { localDate, string } from 'lib-common/form/fields'
import { mapped, nullBlank, object, validated } from 'lib-common/form/form'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import {
  AnsweredQuestion,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import LocalDate from 'lib-common/local-date'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'

import { useTranslations } from '../../i18n'
import { DatePickerF } from '../../molecules/date-picker/DatePicker'

import { DocumentQuestionDescriptor, TemplateQuestionDescriptor } from './types'

const questionType: QuestionType = 'DATE'

type ApiQuestion = Question.DateQuestion

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

type Answer = LocalDate | null

const questionForm = mapped(
  object({
    template: templateForm,
    answer: nullBlank(localDate())
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
): StateOf<QuestionForm>['answer'] =>
  answer !== undefined ? localDate.fromDate(answer) : localDate.fromDate(null)

const View = React.memo(function View({
  bind,
  readOnly
}: {
  bind: BoundForm<QuestionForm>
  readOnly: boolean
}) {
  const { template, answer } = useFormFields(bind)
  const { label, infoText } = useFormFields(template)
  return readOnly ? (
    <FixedSpaceColumn data-qa="document-question-preview">
      <Label>{label.state}</Label>
      <div>{answer.value()?.format() ?? '-'}</div>
    </FixedSpaceColumn>
  ) : (
    <FixedSpaceColumn fullWidth data-qa="document-question">
      <ExpandingInfo info={infoText.value()} width="full">
        <Label>{label.state}</Label>
      </ExpandingInfo>
      <DatePickerF bind={answer} locale="fi" data-qa="answer-input" />
    </FixedSpaceColumn>
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
    answer: getAnswerState(null)
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
        <InputFieldF
          bind={label}
          hideErrorsBeforeTouched
          data-qa="question-label-input"
        />
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
      answer: getAnswerState(answer)
    }
  }),
  Component: View
}

export default {
  template: templateQuestionDescriptor,
  document: documentQuestionDescriptor
}
