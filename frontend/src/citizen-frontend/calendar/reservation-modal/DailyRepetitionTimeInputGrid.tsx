// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm, useFormFields, useFormUnion } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { Label } from 'lib-components/typography'

import { HolidayReservation, Times } from './TimeInputs'
import { dailyTimes } from './form'

export interface DailyRepetitionTimeInputGridProps {
  bind: BoundForm<typeof dailyTimes>
  anyChildInShiftCare: boolean
  showAllErrors: boolean
}

export default React.memo(function DailyRepetitionTimeInputGrid({
  bind,
  showAllErrors,
  anyChildInShiftCare
}: DailyRepetitionTimeInputGridProps) {
  const i18n = useTranslation()

  const { weekDayRange, reservation } = useFormFields(bind)
  const { branch, form } = useFormUnion(reservation)

  if (weekDayRange.state === undefined) {
    return <div>{i18n.calendar.reservationModal.noReservableDays}</div>
  }

  const [firstWeekDay, lastWeekDay] = weekDayRange.state
  const label = (
    <Label>{`${i18n.common.datetime.weekdaysShort[firstWeekDay - 1]}-${
      i18n.common.datetime.weekdaysShort[lastWeekDay - 1]
    }`}</Label>
  )

  switch (branch) {
    case 'times':
      return (
        <Times
          bind={form}
          label={label}
          showAllErrors={showAllErrors}
          allowExtraTimeRange={anyChildInShiftCare}
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
