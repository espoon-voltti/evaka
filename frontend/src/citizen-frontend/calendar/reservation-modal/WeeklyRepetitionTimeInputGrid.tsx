// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import React, { useEffect } from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import type { TimeRanges } from 'lib-common/reservations'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { LabelLike } from 'lib-components/typography'

import type { RepetitionTimeInputGridProps } from './RepetitionTimeInputGrid'
import TimeInputs from './TimeInputs'
import {
  allChildrenAreAbsent,
  allChildrenHaveDayOff,
  bindUnboundedTimeRanges,
  emptyTimeRange,
  getCommonTimeRanges,
  hasReservationsForEveryChild
} from './utils'

export default React.memo(function WeeklyRepetitionTimeInputGrid({
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

    const groupedDays = groupBy(
      [...selectedRange.dates()],
      (date) => date.getIsoDayOfWeek() - 1
    )

    updateForm({
      weeklyTimes: Array(7)
        .fill(undefined)
        .map((_, dayOfWeek) => {
          const dayOfWeekDays = groupedDays[dayOfWeek]

          if (!dayOfWeekDays) {
            return undefined
          }

          const relevantReservations = reservations.filter(({ date }) =>
            dayOfWeekDays.some((d) => d.isEqual(date))
          )

          if (
            allChildrenHaveDayOff(
              relevantReservations,
              formData.selectedChildren
            )
          ) {
            return 'day-off'
          }

          if (
            allChildrenAreAbsent(
              relevantReservations,
              formData.selectedChildren
            )
          ) {
            return 'absent'
          }

          if (
            !hasReservationsForEveryChild(
              relevantReservations,
              formData.selectedChildren
            )
          ) {
            return emptyTimeRange
          }

          const commonTimeRanges = getCommonTimeRanges(
            relevantReservations,
            formData.selectedChildren
          )

          if (commonTimeRanges) {
            return bindUnboundedTimeRanges(commonTimeRanges)
          }

          return emptyTimeRange
        })
    })
  }, [
    existingReservations,
    formData.selectedChildren,
    selectedRange,
    updateForm
  ])

  return (
    <>
      {formData.weeklyTimes
        .map(
          (times, index) =>
            [times, index] as [TimeRanges | 'absent' | undefined, number]
        )
        .filter(([times, index]) => times && includedDays.includes(index + 1))
        .map(([times, index]) => (
          <TimeInputs
            key={`day-${index}`}
            label={
              <LabelLike>{i18n.common.datetime.weekdaysShort[index]}</LabelLike>
            }
            times={times}
            updateTimes={(times) =>
              updateForm({
                weeklyTimes: [
                  ...formData.weeklyTimes.slice(0, index),
                  times,
                  ...formData.weeklyTimes.slice(index + 1)
                ]
              })
            }
            validationErrors={validationResult.errors?.weeklyTimes?.[index]}
            showAllErrors={showAllErrors}
            allowExtraTimeRange={childrenInShiftCare}
            dataQaPrefix={`weekly-${index}`}
            onFocus={(ev) => {
              scrollIntoViewSoftKeyboard(ev.target)
            }}
            showAbsences
          />
        ))}
    </>
  )
})
