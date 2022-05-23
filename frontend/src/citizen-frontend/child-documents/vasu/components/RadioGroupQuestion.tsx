// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { RadioGroupQuestion } from 'lib-common/api-types/vasu'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'

interface Props {
  questionNumber: string
  question: RadioGroupQuestion
  selectedValue: string | null
  translations: VasuTranslations
}

export function RadioGroupQuestion({
  question: { name, options },
  questionNumber,
  selectedValue,
  translations
}: Props) {
  return (
    <div>
      <Label>
        {questionNumber} {name}
      </Label>

      <ValueOrNoRecord
        text={options.find((option) => option.key === selectedValue)?.name}
        translations={translations}
      />
    </div>
  )
}
