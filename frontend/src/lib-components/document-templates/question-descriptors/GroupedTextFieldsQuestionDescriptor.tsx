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
  Question,
  QuestionType
} from 'lib-common/generated/api-types/document'
import { Button } from 'lib-components/atoms/buttons/Button'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'

import { IconOnlyButton } from '../../atoms/buttons/IconOnlyButton'
import { useTranslations } from '../../i18n'

import { DocumentQuestionDescriptor, TemplateQuestionDescriptor } from './types'

const questionType: QuestionType = 'GROUPED_TEXT_FIELDS'

type ApiQuestion = Question.GroupedTextFieldsQuestion

const templateForm = object({
  id: validated(string(), nonBlank),
  label: validated(string(), nonBlank),
  fieldLabels: validated(array(validated(string(), nonBlank)), (arr) =>
    arr.length > 0 ? undefined : 'required'
  ),
  infoText: string(),
  allowMultipleRows: boolean()
})

type TemplateForm = typeof templateForm

const getTemplateInitialValues = (
  question?: ApiQuestion
): StateOf<TemplateForm> => ({
  id: question?.id ?? uuidv4(),
  label: question?.label ?? '',
  fieldLabels: question?.fieldLabels ?? [''],
  infoText: question?.infoText ?? '',
  allowMultipleRows: question?.allowMultipleRows ?? false
})

type Answer = string[][]

const rowForm = array(value<string>())

const questionForm = mapped(
  object({
    template: templateForm,
    answer: array(rowForm)
  }),
  (output): AnsweredQuestion => ({
    questionId: output.template.id,
    answer: output.answer,
    type: questionType
  })
)

type QuestionForm = typeof questionForm

const getAnswerState = (
  numberOfFields: number,
  answer?: Answer | undefined
): StateOf<QuestionForm>['answer'] =>
  answer !== undefined ? answer : [Array<string>(numberOfFields).fill('')]

const FixedWidthDiv = styled.div<{ $questionCount: number }>`
  width: ${(p) => (p.$questionCount > 3 ? 200 : 270)}px;
  max-width: ${(p) => (p.$questionCount > 3 ? 200 : 270)}px;
`

const Indentation = styled.div`
  margin-left: 24px;
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
  const { label, infoText, fieldLabels } = useFormFields(template)
  const answerRows = useFormElems(answer)

  return readOnly ? (
    <FixedSpaceColumn data-qa="document-question-preview">
      <Label>{label.state}</Label>
      <Indentation>
        <FixedSpaceColumn>
          {answer.state.map((row, i) => (
            <FixedSpaceColumn key={i}>
              <FixedSpaceRow>
                {fieldLabels.state.map((label) => (
                  <FixedWidthDiv
                    key={label}
                    $questionCount={fieldLabels.state.length}
                  >
                    <Label>{label}</Label>
                  </FixedWidthDiv>
                ))}
              </FixedSpaceRow>
              <FixedSpaceRow>
                {row.map((value, j) => (
                  <FixedWidthDiv
                    key={j}
                    $questionCount={fieldLabels.state.length}
                  >
                    {value || '-'}
                  </FixedWidthDiv>
                ))}
              </FixedSpaceRow>
            </FixedSpaceColumn>
          ))}
        </FixedSpaceColumn>
      </Indentation>
    </FixedSpaceColumn>
  ) : (
    <FixedSpaceColumn fullWidth data-qa="document-question">
      <ExpandingInfo info={infoText.value()} width="full">
        <Label>{label.state}</Label>
      </ExpandingInfo>
      <Indentation>
        <FixedSpaceColumn>
          {answerRows.map((row, i) => (
            <FixedSpaceColumn key={i}>
              <FixedSpaceRow>
                {fieldLabels.state.map((label) => (
                  <FixedWidthDiv
                    key={label}
                    $questionCount={fieldLabels.state.length}
                  >
                    <Label>{label}</Label>
                  </FixedWidthDiv>
                ))}
              </FixedSpaceRow>
              <FixedSpaceRow>
                <QuestionRow bind={row} />
                {answerRows.length > 1 && (
                  <IconOnlyButton
                    icon={faTrash}
                    aria-label={i18n.common.remove}
                    onClick={() =>
                      answer.update((prev) => [
                        ...prev.slice(0, i),
                        ...prev.slice(i + 1)
                      ])
                    }
                  />
                )}
              </FixedSpaceRow>
            </FixedSpaceColumn>
          ))}
          {template.state.allowMultipleRows && (
            <Button
              appearance="inline"
              onClick={() =>
                answer.update((prev) => [
                  ...prev,
                  Array<string>(template.state.fieldLabels.length).fill('')
                ])
              }
              text={i18n.documentTemplates.templateQuestions.addRow}
              icon={faPlus}
            />
          )}
        </FixedSpaceColumn>
      </Indentation>
    </FixedSpaceColumn>
  )
})

const QuestionRow = React.memo(function Foo({
  bind
}: {
  bind: BoundForm<typeof rowForm>
}) {
  const fields = useFormElems(bind)
  return (
    <FixedSpaceRow>
      {fields.map((field, i) => (
        <FixedWidthDiv key={i} $questionCount={fields.length}>
          <InputFieldF bind={field} />
        </FixedWidthDiv>
      ))}
    </FixedSpaceRow>
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
    answer: getAnswerState(bind.state.fieldLabels.length)
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
  const { label, infoText, fieldLabels, allowMultipleRows } =
    useFormFields(bind)
  const fieldLabelsElems = useFormElems(fieldLabels)

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
        <Label>{i18n.documentTemplates.templateQuestions.textFields}</Label>
        <FixedSpaceColumn>
          {fieldLabelsElems.map((elem, i) => (
            <FixedSpaceRow key={i}>
              <InputFieldF bind={elem} key={i} />
              {fieldLabelsElems.length > 1 && (
                <IconOnlyButton
                  icon={faTrash}
                  onClick={() =>
                    fieldLabels.update((prev) => [
                      ...prev.slice(0, i),
                      ...prev.slice(i + 1)
                    ])
                  }
                  aria-label={i18n.common.remove}
                />
              )}
            </FixedSpaceRow>
          ))}
          <Button
            appearance="inline"
            onClick={() => fieldLabels.update((prev) => [...prev, ''])}
            text={i18n.documentTemplates.templateQuestions.addTextField}
            icon={faPlus}
            disabled={fieldLabelsElems.length >= 4}
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>{i18n.documentTemplates.templateQuestions.infoText}</Label>
        <InputFieldF bind={infoText} hideErrorsBeforeTouched />
      </FixedSpaceColumn>
      <CheckboxF
        bind={allowMultipleRows}
        label={i18n.documentTemplates.templateQuestions.allowMultipleRows}
      />
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
      answer: getAnswerState(question.fieldLabels.length, answer)
    }
  }),
  Component: View
}

export default {
  template: templateQuestionDescriptor,
  document: documentQuestionDescriptor
}
