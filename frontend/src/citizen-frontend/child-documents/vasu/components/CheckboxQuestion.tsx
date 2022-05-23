// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { CheckboxQuestion } from 'lib-common/api-types/vasu'
import { VasuTranslations } from 'lib-customizations/employee'

import { useTranslation } from '../../../localization'

import { ReadOnlyValue } from './ReadOnlyValue'
import { QuestionProps } from './question-props'

type Props = QuestionProps<CheckboxQuestion> & {
  translations: VasuTranslations
}

export function CheckboxQuestion(props: Props) {
  const i18n = useTranslation()
  const label = `${props.questionNumber} ${props.question.name}`
  return (
    <ReadOnlyValue
      label={label}
      value={props.question.value ? i18n.common.yes : i18n.common.no}
      translations={props.translations}
    />
  )
}
