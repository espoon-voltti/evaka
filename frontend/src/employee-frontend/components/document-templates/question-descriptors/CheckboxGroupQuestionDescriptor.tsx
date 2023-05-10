// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faTrash } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { array, mapped, object, validated, value } from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonEmpty } from 'lib-common/form/validators'
import {
  AnsweredQuestion,
  Question
} from 'lib-common/generated/api-types/document'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'

import {
  DocumentQuestionDescriptor,
  QuestionType,
  TemplateQuestionDescriptor
} from './types'

const questionType: QuestionType = 'CHECKBOX_GROUP'

type ApiQuestion = Question.CheckboxGroupQuestion

const optionForm = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty)
})

const templateForm = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty),
  options: validated(array(optionForm), (arr) =>
    arr.length > 0 ? undefined : 'required'
  )
})

type TemplateForm = typeof templateForm

const getTemplateInitialValues = (
  question?: ApiQuestion
): StateOf<TemplateForm> => ({
  id: question?.id ?? crypto.randomUUID(),
  label: question?.label ?? '',
  options: question?.options ?? []
})

type Answer = string[]

const getAnswerInitialValue = (): Answer => []

const questionForm = mapped(
  object({
    template: templateForm,
    answer: value<Answer>()
  }),
  (output): AnsweredQuestion => ({
    questionId: output.template.id,
    answer: output.answer
  })
)

type QuestionForm = typeof questionForm

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
  const { template, answer } = useFormFields(bind)
  const { label, options } = useFormFields(template)
  const optionElems = useFormElems(options)
  const selected = answer.state.map(
    (answer) => optionElems.find((opt) => opt.state.id === answer)?.state?.label
  )

  return readOnly ? (
    <FixedSpaceColumn>
      <Label>{label.state}</Label>
      <div>{selected.length > 0 ? selected.join(', ') : '-'}</div>
    </FixedSpaceColumn>
  ) : (
    <FixedSpaceColumn>
      <Label>{label.state}</Label>
      <GroupIndentation>
        <FixedSpaceColumn spacing="xs">
          {optionElems.map((option) => (
            <Checkbox
              key={option.state.id}
              label={option.state.label}
              checked={answer.state.some((id) => id === option.state.id)}
              onChange={(checked) =>
                answer.update((old) => [
                  ...old.filter((id) => id !== option.state.id),
                  ...(checked ? [option.state.id] : [])
                ])
              }
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
  const { i18n } = useTranslation()
  const mockBind = useForm(
    questionForm,
    () => ({
      template: bind.state,
      answer: getAnswerInitialValue()
    }),
    i18n.validationErrors
  )

  return <View bind={mockBind} readOnly={false} />
})

const OptionView = React.memo(function OptionView({
  bind,
  onDelete
}: {
  bind: BoundForm<typeof optionForm>
  onDelete: () => void
}) {
  const { i18n } = useTranslation()
  const { label } = useFormFields(bind)

  return (
    <FixedSpaceRow>
      <InputFieldF bind={label} hideErrorsBeforeTouched />
      <IconButton
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
  const { i18n } = useTranslation()
  const { label, options } = useFormFields(bind)
  const optionElems = useFormElems(options)

  return (
    <FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.label}</Label>
        <InputFieldF bind={label} hideErrorsBeforeTouched />
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.options}</Label>
        {optionElems.map((opt) => (
          <FixedSpaceRow key={opt.state.id}>
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
        <IconButton
          icon={faPlus}
          aria-label={i18n.common.add}
          onClick={() => {
            options.update((old) => [
              ...old,
              {
                id: crypto.randomUUID(),
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
      answer: answer ?? getAnswerInitialValue()
    }
  }),
  Component: View
}

export default {
  template: templateQuestionDescriptor,
  document: documentQuestionDescriptor
}
