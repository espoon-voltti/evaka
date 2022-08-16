// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { DateQuestion } from 'lib-common/api-types/vasu'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<DateQuestion> {
  translations: VasuTranslations
}

export default React.memo(function DateQuestion({
  question: { name, value },
  questionNumber,
  translations
}: Props) {
  return (
    <FixedSpaceColumn spacing="xxs">
      <Label>
        {questionNumber} {name}
      </Label>
      <ValueOrNoRecord text={value?.format()} translations={translations} />
    </FixedSpaceColumn>
  )
})
