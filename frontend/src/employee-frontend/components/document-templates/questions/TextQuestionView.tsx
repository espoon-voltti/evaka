import React from 'react'

import { value } from 'lib-common/form/form'
import { BoundFormState, useForm } from 'lib-common/form/hooks'
import { Question } from 'lib-common/generated/api-types/document'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'

interface Props {
  question: Question.TextQuestion
  bind?: BoundFormState<string>
  readOnly?: boolean
}

export default React.memo(function TextQuestionView({
  question,
  bind,
  readOnly
}: Props) {
  const { i18n } = useTranslation()
  const defaultBind = useForm(value<string>(), () => '', i18n.validationErrors)

  return (
    <div>
      <FixedSpaceColumn>
        <Label>{question.label}</Label>
        <InputFieldF bind={bind ?? defaultBind} readonly={readOnly} width="L" />
      </FixedSpaceColumn>
    </div>
  )
})
