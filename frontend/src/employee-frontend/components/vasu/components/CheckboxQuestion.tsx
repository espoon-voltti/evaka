// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { useTranslation } from '../../../state/i18n'
import { CheckboxQuestion } from '../vasu-content'
import { QuestionProps } from './question-props'
import { ReadOnlyValue } from './ReadOnlyValue'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'

type Props = QuestionProps<CheckboxQuestion> & {
  onChange?: (checked: boolean) => void
}

export function CheckboxQuestion(props: Props) {
  const { i18n } = useTranslation()
  const label = `${props.questionNumber} ${props.question.name}`
  return (
    <ExpandingInfo
      info={
        props.question.info.length ? <div>{props.question.info}</div> : null
      }
      ariaLabel={i18n.common.openExpandingInfo}
    >
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
        />
      )}
    </ExpandingInfo>
  )
}
