// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { Label } from 'lib-components/typography'

import type { RepetitionTimeInputGridProps } from './RepetitionTimeInputGrid'
import TimeInputs from './TimeInputs'
import {
  bindUnboundedTimeRanges,
  emptyTimeRange,
  getCommonTimeRanges,
  hasReservationsForEveryChild
} from './utils'

export default React.memo(function DailyRepetitionTimeInputGrid({
  formData,
  updateForm,
  showAllErrors,
  childrenInShiftCare,
  includedDays,
  existingReservations,
  validationResult,
  selectedRange
}: RepetitionTimeInputGridProps) {
  const i18n = useTranslation()

  useEffect(() => {
    if (!selectedRange) return

    const reservations = existingReservations.filter((reservation) =>
      selectedRange.includes(reservation.date)
    )

    if (
      !hasReservationsForEveryChild(reservations, formData.selectedChildren)
    ) {
      updateForm({ dailyTimes: emptyTimeRange })
      return
    }

    const commonTimeRanges = getCommonTimeRanges(
      reservations,
      formData.selectedChildren
    )

    if (commonTimeRanges) {
      updateForm({
        dailyTimes: bindUnboundedTimeRanges(commonTimeRanges)
      })
    }
  }, [
    existingReservations,
    formData.selectedChildren,
    selectedRange,
    updateForm
  ])

  const label = (
    <Label>{`${i18n.common.datetime.weekdaysShort[includedDays[0] - 1]}-${
      i18n.common.datetime.weekdaysShort[
        includedDays[includedDays.length - 1] - 1
      ]
    }`}</Label>
  )

  if (formData.dailyTimes === 'day-off') {
    return (
      <>
        <div>{label}</div>
        <div>{i18n.calendar.reservationModal.dayOff}</div>
        <div />
      </>
    )
  }

  return (
    <TimeInputs
      label={label}
      times={formData.dailyTimes}
      updateTimes={(dailyTimes) => updateForm({ dailyTimes })}
      validationErrors={validationResult.errors?.dailyTimes}
      showAllErrors={showAllErrors}
      allowExtraTimeRange={childrenInShiftCare}
      dataQaPrefix="daily"
      onFocus={(ev) => {
        scrollIntoViewSoftKeyboard(ev.target)
      }}
      showAbsences={false}
    />
  )
})
