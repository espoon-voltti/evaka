// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { RadioGroupQuestion } from 'lib-common/api-types/vasu'
import type LocalDate from 'lib-common/local-date'
import { Label } from 'lib-components/typography'
import type { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'

interface Props {
  questionNumber: string
  question: RadioGroupQuestion
  selectedValue: {
    key: string
    range: {
      start: LocalDate
      end: LocalDate
    } | null
  } | null
  translations: VasuTranslations
}

export function RadioGroupQuestion({
  question: { name, options },
  questionNumber,
  selectedValue,
  translations
}: Props) {
  const selectedOption = options.find(
    (option) => option.key === selectedValue?.key
  )
  const selectedDateRange = selectedValue?.range
    ? ` ${selectedValue.range.start.format()}–${selectedValue.range.end.format()}`
    : ''

  return (
    <div>
      <Label>
        {questionNumber} {name}
      </Label>

      <ValueOrNoRecord
        text={selectedOption && `${selectedOption.name}${selectedDateRange}`}
        translations={translations}
      />
    </div>
  )
}
