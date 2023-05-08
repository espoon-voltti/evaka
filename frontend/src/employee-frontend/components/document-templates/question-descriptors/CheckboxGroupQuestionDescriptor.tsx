// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faTrash } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { array, object, validated, value } from 'lib-common/form/form'
import { BoundForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { nonEmpty } from 'lib-common/form/validators'
import { Question } from 'lib-common/generated/api-types/document'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { QuestionDescriptor, QuestionType, BoundViewProps } from './types'

const questionType: QuestionType = 'CHECKBOX_GROUP'

type ApiQuestion = Question.CheckboxGroupQuestion

type AnswerType = string[]

const getAnswerInitialValue = (): AnswerType => []

const optionForm = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty)
})

const form = object({
  id: validated(string(), nonEmpty),
  label: validated(string(), nonEmpty),
  options: validated(array(optionForm), (arr) =>
    arr.length > 0 ? undefined : 'required'
  ),
  answer: value<AnswerType>()
})

const getInitialState = (question?: ApiQuestion): StateOf<typeof form> => ({
  id: question?.id ?? crypto.randomUUID(),
  label: question?.label ?? '',
  options: question?.options ?? [],
  answer: getAnswerInitialValue()
})

const GroupIndentation = styled.div`
  margin-left: ${defaultMargins.s};
`

const View = React.memo(function View({ bind }: BoundViewProps<typeof form>) {
  const { label, options, answer } = useFormFields(bind)
  const optionElems = useFormElems(options)
  return (
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

const ReadOnlyView = React.memo(function ReadOnlyView({
  bind
}: BoundViewProps<typeof form>) {
  const { label, options, answer } = useFormFields(bind)
  const optionElems = useFormElems(options)
  const selected = answer.state.map(
    (answer) => optionElems.find((opt) => opt.state.id === answer)?.state?.label
  )

  return (
    <FixedSpaceColumn>
      <Label>{label.state}</Label>
      <div>{selected.length > 0 ? selected.join(', ') : '-'}</div>
    </FixedSpaceColumn>
  )
})

const OptionView = React.memo(function OptionView({
  bind,
  onDelete
}: {
  bind: BoundForm<typeof optionForm>
  onDelete: () => void
}) {
  const { label } = useFormFields(bind)

  return (
    <FixedSpaceRow>
      <InputFieldF bind={label} hideErrorsBeforeTouched />
      <IconButton icon={faTrash} aria-label="Poista" onClick={onDelete} />
    </FixedSpaceRow>
  )
})

const TemplateView = React.memo(function TemplateView({
  bind
}: BoundViewProps<typeof form>) {
  const { label, options } = useFormFields(bind)
  const optionElems = useFormElems(options)

  return (
    <FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>Otsikko</Label>
        <InputFieldF bind={label} hideErrorsBeforeTouched />
      </FixedSpaceColumn>
      <FixedSpaceColumn>
        <Label>Vaihtoehdot</Label>
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
          aria-label="Lisää"
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
