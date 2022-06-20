// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { DateQuestion } from 'lib-common/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

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
  const { i18n, lang } = useTranslation()

  const [date, setDate] = useState<LocalDate | null>(null)

  return (
    <FixedSpaceColumn spacing="xxs">
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>
      {onChange ? (
        <DatePicker
          date={date}
          onChange={(d) => {
            onChange(d)
            setDate(d)
          }}
          locale={lang}
          data-qa="date-question-picker"
          errorTexts={i18n.validationErrors}
        />
      ) : (
        <ValueOrNoRecord text={value?.format()} translations={translations} />
      )}
    </FixedSpaceColumn>
  )
})
