// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { QuestionOption, RadioGroupQuestion } from '../vasu-content'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { useTranslation } from '../../../state/i18n'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'

interface Props {
  questionNumber: string
  question: RadioGroupQuestion
  selectedValue: string | null
  onChange?: (selected: QuestionOption) => void
}

export function RadioGroupQuestion({
  onChange,
  question: { name, options, info },
  questionNumber,
  selectedValue
}: Props) {
  const { i18n } = useTranslation()

  return (
    <>
      <ExpandingInfo
        info={info.length ? <div>{info}</div> : null}
        ariaLabel={i18n.common.openExpandingInfo}
      >
        <Label>
          {questionNumber} {name}
        </Label>
      </ExpandingInfo>

      {onChange ? (
        <>
          <Gap size={'m'} />
          <FixedSpaceColumn>
            {options.map((option) => (
              <Radio
                key={option.key}
                checked={selectedValue === option.key}
                label={option.name}
                onChange={() => onChange(option)}
              />
            ))}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={options.find((option) => option.key === selectedValue)?.name}
        />
      )}
    </>
  )
}
