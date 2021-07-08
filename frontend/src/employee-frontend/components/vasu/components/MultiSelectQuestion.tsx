// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Checkbox from '../../../../lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from '../../../../lib-components/layout/flex-helpers'
import { Label } from '../../../../lib-components/typography'
import { Gap } from '../../../../lib-components/white-space'
import { MultiSelectQuestion, QuestionOption } from '../vasu-content'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiSelectQuestion> {
  selectedValues: string[]
  onChange?: (option: QuestionOption, checked: boolean) => void
}

export function MultiSelectQuestion({
  onChange,
  question: { name, options },
  questionNumber,
  selectedValues
}: Props) {
  return (
    <>
      <Label>
        {questionNumber} {name}
      </Label>
      {onChange ? (
        <>
          <Gap size={'m'} />
          <FixedSpaceColumn>
            {options.map((option) => {
              return (
                <Checkbox
                  key={option.key}
                  checked={selectedValues.includes(option.key)}
                  label={option.name}
                  onChange={(checked) => onChange(option, checked)}
                />
              )
            })}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={options
            .filter((option) => selectedValues.includes(option.key))
            .map((o) => o.name)
            .join(', ')}
        />
      )}
    </>
  )
}
