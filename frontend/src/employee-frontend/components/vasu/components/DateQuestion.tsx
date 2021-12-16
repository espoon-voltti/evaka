// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from 'employee-frontend/state/i18n'
import { DateQuestion } from 'lib-common/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'
import React, { useEffect, useState } from 'react'
import QuestionInfo from '../QuestionInfo'
import { QuestionProps } from './question-props'
import { ValueOrNoRecord } from './ValueOrNoRecord'

interface Props extends QuestionProps<DateQuestion> {
  onChange?: (value: LocalDate | null) => void
  translations: VasuTranslations
}

export default React.memo(function DateQuestion({
  question: { info, name, value },
  questionNumber,
  onChange,
  translations
}: Props) {
  const { lang } = useTranslation()
  const [inputValue, setInputValue] = useState(value?.format() ?? '')

  useEffect(() => {
    if (onChange) {
      const date = LocalDate.parseFiOrNull(inputValue)
      // Update actual date value only when input has valid content
      if (date || !inputValue) {
        onChange(date)
      }
    }
  }, [inputValue]) // eslint-disable-line react-hooks/exhaustive-deps

  const forceDate = () => {
    const parsed = LocalDate.parseFiOrNull(inputValue)
    setInputValue(parsed?.format() ?? '')
  }

  return (
    <FixedSpaceColumn spacing="xxs">
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>
      {onChange ? (
        <DatePicker
          date={inputValue}
          onChange={setInputValue}
          onBlur={forceDate}
          locale={lang}
        />
      ) : (
        <ValueOrNoRecord text={value?.format()} translations={translations} />
      )}
    </FixedSpaceColumn>
  )
})
