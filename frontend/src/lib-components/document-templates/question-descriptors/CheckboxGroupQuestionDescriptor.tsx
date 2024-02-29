// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faTrash } from 'Icons'
import React, { useState } from 'react'
import styled from 'styled-components'
import { v4 as uuidv4 } from 'uuid'

import { boolean, string } from 'lib-common/form/fields'
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
  CheckboxGroupAnswerContent,
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Checkbox, { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import InputField, { InputFieldF } from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslations } from '../../i18n'

import { DocumentQuestionDescriptor, TemplateQuestionDescriptor } from './types'

const questionType: QuestionType = 'CHECKBOX_GROUP'

type ApiQuestion = Question.CheckboxGroupQuestion

const optionForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank),
  withText: boolean()
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

type Answer = CheckboxGroupAnswerContent[]

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
): StateOf<QuestionForm>['answer'] => (answer !== undefined ? answer : [])

const GroupIndentation = styled.div`
  margin-left: ${defaultMargins.s};
`

const NoShrink = styled.div`
  flex-shrink: 0;
`

const View = React.memo(function View({
  bind,
  readOnly
}: {
  bind: BoundForm<QuestionForm>
  readOnly: boolean
}) {
  const { template, answer } = useFormFields(bind)
  const { label, infoText, options } = useFormFields(template)
  const optionElems = useFormElems(options)

  return readOnly ? (
    <FixedSpaceColumn>
      <Label>{label.state}</Label>
      <ul>
        {answer.state.map((answer) => {
          const option = optionElems.find(
            (opt) => opt.state.id === answer.optionId
          )?.state
          return (
            option && (
              <li key={option.id}>
                {option.label}
                {option.withText ? ` : ${answer.extra}` : ''}
              </li>
            )
          )
        })}
      </ul>
    </FixedSpaceColumn>
  ) : (
    <FixedSpaceColumn fullWidth>
      <ExpandingInfo info={infoText.value()} width="full">
        <Label>{label.state}</Label>
      </ExpandingInfo>
      <GroupIndentation>
        <FixedSpaceColumn spacing="xs">
          {optionElems.map((option) => {
            const answerOption = answer.state.find(
              (opt) => opt.optionId === option.state.id
            )

            return (
              <FixedSpaceRow key={option.state.id} alignItems="center">
                <NoShrink>
                  <Checkbox
                    label={option.state.label}
                    checked={answerOption !== undefined}
                    onChange={(checked) =>
                      answer.update((old) => [
                        ...old.filter(
                          (opt) => opt.optionId !== option.state.id
                        ),
                        ...(checked
                          ? [{ optionId: option.state.id, extra: '' }]
                          : [])
                      ])
                    }
                  />
                </NoShrink>
                {option.state.withText && (
                  <InputField
                    value={answerOption?.extra ?? ''}
                    readonly={!answerOption}
                    onChange={(value) =>
                      answer.update((old) => [
                        ...old.filter(
                          (opt) => opt.optionId !== option.state.id
                        ),
                        { optionId: option.state.id, extra: value }
                      ])
                    }
                    width="L"
                  />
                )}
              </FixedSpaceRow>
            )
          })}
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
  onDelete,
  index
}: {
  bind: BoundForm<typeof optionForm>
  onDelete: () => void
  index: number
}) {
  const i18n = useTranslations()
  const { label, withText } = useFormFields(bind)

  return (
    <FixedSpaceRow alignItems="baseline">
      <span>{index + 1}.</span>
      <FixedSpaceColumn spacing="xxs">
        <FixedSpaceRow alignItems="center">
          <InputFieldF bind={label} hideErrorsBeforeTouched width="m" />
          <IconButton
            icon={faTrash}
            aria-label={i18n.common.remove}
            onClick={onDelete}
          />
        </FixedSpaceRow>
        <CheckboxF
          bind={withText}
          label={i18n.documentTemplates.templateQuestions.withText}
        />
      </FixedSpaceColumn>
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
        {optionElems.map((opt, i) => (
          <FixedSpaceRow key={opt.state.id} alignItems="center">
            <OptionView
              index={i}
              bind={opt}
              onDelete={() =>
                options.update((old) =>
                  old.filter(({ id }) => id !== opt.state.id)
                )
              }
            />
          </FixedSpaceRow>
        ))}
        <IconButton
          icon={faPlus}
          aria-label={i18n.common.add}
          onClick={() => {
            options.update((old) => [
              ...old,
              {
                id: uuidv4(),
                label: '',
                withText: false
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
