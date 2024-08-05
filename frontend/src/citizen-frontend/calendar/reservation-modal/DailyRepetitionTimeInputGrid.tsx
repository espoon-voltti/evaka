// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { Label } from 'lib-components/typography'

import { Day } from './TimeInputs'
import { type dailyTimes } from './form'

export interface DailyRepetitionTimeInputGridProps {
  bind: BoundForm<typeof dailyTimes>
  showAllErrors: boolean
}

export default React.memo(function DailyRepetitionTimeInputGrid({
  bind,
  showAllErrors
}: DailyRepetitionTimeInputGridProps) {
  const i18n = useTranslation()

  const { weekDayRange, day } = useFormFields(bind)
  const [firstWeekDay, lastWeekDay] = weekDayRange.state

  const label = (
    <Label>{`${i18n.common.datetime.weekdaysShort[firstWeekDay - 1]}-${
      i18n.common.datetime.weekdaysShort[lastWeekDay - 1]
    }`}</Label>
  )

  return (
    <Day
      bind={day}
      label={label}
      showAllErrors={showAllErrors}
      dataQaPrefix="daily"
      onFocus={(ev) => {
        scrollIntoViewSoftKeyboard(ev.target)
      }}
    />
  )
})
