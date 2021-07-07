// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Label } from 'lib-components/typography'
import React from 'react'
import Checkbox from '../../../../lib-components/atoms/form/Checkbox'
import { useTranslation } from '../../../state/i18n'
import { CheckboxQuestion } from '../vasu-content'
import { QuestionProps } from './question-props'

type Props = QuestionProps<CheckboxQuestion> & {
  onChange?: (checked: boolean) => void
}

export function CheckboxQuestion(props: Props) {
  const { i18n } = useTranslation()
  const label = `${props.questionNumber} ${props.question.name}`
  return props.onChange ? (
    <Checkbox
      checked={props.question.value}
      label={label}
      onChange={props.onChange}
    />
  ) : (
    <>
      <Label>{label}</Label>
      <div>{props.question.value ? i18n.common.yes : i18n.common.no}</div>
    </>
  )
}
