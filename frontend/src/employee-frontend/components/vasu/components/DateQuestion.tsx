// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import type { DateQuestion } from 'lib-common/api-types/vasu'
import type LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label } from 'lib-components/typography'
import type { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import type { QuestionProps } from './question-props'

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
  return (
    <FixedSpaceColumn spacing="xxs">
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>
      {onChange ? (
        <DatePicker
          date={value}
          onChange={onChange}
          locale={lang}
          data-qa="date-question-picker"
        />
      ) : (
        <ValueOrNoRecord text={value?.format()} translations={translations} />
      )}
    </FixedSpaceColumn>
  )
})
