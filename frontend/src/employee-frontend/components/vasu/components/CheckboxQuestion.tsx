// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { useTranslation } from '../../../state/i18n'
import { CheckboxQuestion } from 'lib-common/api-types/vasu'
import { QuestionProps } from './question-props'
import { ReadOnlyValue } from './ReadOnlyValue'
import QuestionInfo from '../QuestionInfo'
import { VasuTranslations } from 'lib-customizations/employee'

type Props = QuestionProps<CheckboxQuestion> & {
  onChange?: (checked: boolean) => void
  translations: VasuTranslations
}

export function CheckboxQuestion(props: Props) {
  const { i18n } = useTranslation()
  const label = `${props.questionNumber} ${props.question.name}`
  return (
    <QuestionInfo info={props.question.info}>
      {props.onChange ? (
        <Checkbox
          checked={props.question.value}
          label={label}
          onChange={props.onChange}
        />
      ) : (
        <ReadOnlyValue
          label={label}
          value={props.question.value ? i18n.common.yes : i18n.common.no}
          translations={props.translations}
        />
      )}
    </QuestionInfo>
  )
}
