// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { v4 as uuidv4 } from 'uuid'

import { string } from 'lib-common/form/fields'
import { array, mapped, object, validated, value } from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import {
  AnsweredQuestion,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus, faTrash } from 'lib-icons'

import { useTranslations } from '../../i18n'

import { DocumentQuestionDescriptor, TemplateQuestionDescriptor } from './types'

const questionType: QuestionType = 'RADIO_BUTTON_GROUP'

type ApiQuestion = Question.RadioButtonGroupQuestion

const optionForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank)
})

const templateForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank),
  options: validated(array(optionForm), (arr) =>
    arr.length > 0 ? undefined : 'required'
  ),
  infoText: string()
})

type TemplateForm = typeof templateForm

const getTemplateInitialValues = (
  question?: ApiQuestion
): StateOf<TemplateForm> => ({
  id: question?.id ?? uuidv4(),
  label: question?.label ?? '',
  options: question?.options ?? [],
  infoText: question?.infoText ?? ''
})

type Answer = string | null

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

const GroupIndentation = styled.div`
  margin-left: ${defaultMargins.s};
`

const View = React.memo(function View({
  bind,
  readOnly
}: {
  bind: BoundForm<QuestionForm>
  readOnly: boolean
}) {
  const i18n = useTranslations()
  const { template, answer } = useFormFields(bind)
  const { label, infoText, options } = useFormFields(template)
  const optionElems = useFormElems(options)
  const selected = optionElems.find((opt) => opt.state.id === answer.state)
    ?.state?.label

  return readOnly ? (
    <FixedSpaceColumn>
      <Label>{label.state}</Label>
      <div>{selected ?? i18n.documentTemplates.noSelection}</div>
    </FixedSpaceColumn>
  ) : (
    <FixedSpaceColumn fullWidth>
      <ExpandingInfo info={infoText.value()} width="full">
        <Label>{label.state}</Label>
      </ExpandingInfo>
      <GroupIndentation>
        <FixedSpaceColumn spacing="xs">
          {optionElems.map((option) => (
            <Radio
              key={option.state.id}
              label={option.state.label}
              checked={answer.state === option.state.id}
              onChange={() => answer.set(option.state.id)}
            />
          ))}
        </FixedSpaceColumn>
      </GroupIndentation>
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

const OptionView = React.memo(function OptionView({
  bind,
  onDelete
}: {
  bind: BoundForm<typeof optionForm>
  onDelete: () => void
}) {
  const i18n = useTranslations()
  const { label } = useFormFields(bind)

  return (
    <FixedSpaceRow>
      <InputFieldF bind={label} hideErrorsBeforeTouched />
      <IconOnlyButton
        icon={faTrash}
        aria-label={i18n.common.remove}
        onClick={onDelete}
      />
    </FixedSpaceRow>
  )
})

const TemplateView = React.memo(function TemplateView({
  bind
}: {
  bind: BoundForm<TemplateForm>
}) {
  const i18n = useTranslations()
  const { label, infoText, options } = useFormFields(bind)
  const optionElems = useFormElems(options)

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
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.options}</Label>
        {optionElems.map((opt) => (
          <FixedSpaceRow key={opt.state.id} alignItems="center">
            <OptionView
              bind={opt}
              onDelete={() =>
                options.update((old) =>
                  old.filter(({ id }) => id !== opt.state.id)
                )
              }
            />
          </FixedSpaceRow>
        ))}
        <IconOnlyButton
          icon={faPlus}
          aria-label={i18n.common.add}
          onClick={() => {
            options.update((old) => [
              ...old,
              {
                id: uuidv4(),
                label: ''
              }
            ])
          }}
        />
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
