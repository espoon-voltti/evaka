// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm, useFormUnion } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { Label } from 'lib-components/typography'

import { HolidayReservation, Times } from './TimeInputs'
import { dailyTimes } from './form'

export interface DailyRepetitionTimeInputGridProps {
  bind: BoundForm<typeof dailyTimes>
  childrenInShiftCare: boolean
  includedDays: number[]
  showAllErrors: boolean
}

export default React.memo(function DailyRepetitionTimeInputGrid({
  bind,
  showAllErrors,
  childrenInShiftCare,
  includedDays
}: DailyRepetitionTimeInputGridProps) {
  const i18n = useTranslation()

  const { branch, form } = useFormUnion(bind)

  const label = (
    <Label>{`${i18n.common.datetime.weekdaysShort[includedDays[0] - 1]}-${
      i18n.common.datetime.weekdaysShort[
        includedDays[includedDays.length - 1] - 1
      ]
    }`}</Label>
  )

  switch (branch) {
    case 'times':
      return (
        <Times
          bind={form}
          label={label}
          showAllErrors={showAllErrors}
          allowExtraTimeRange={childrenInShiftCare}
          dataQaPrefix="daily"
          onFocus={(ev) => {
            scrollIntoViewSoftKeyboard(ev.target)
          }}
        />
      )
    case 'holidayReservation':
      return <HolidayReservation bind={form} label={label} />
  }
})
