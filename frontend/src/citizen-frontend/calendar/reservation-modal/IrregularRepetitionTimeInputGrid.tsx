// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect } from 'react'
import styled from 'styled-components'

import { useLang, useTranslation } from 'citizen-frontend/localization'
import { TimeRanges } from 'lib-common/reservations'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { fontWeights, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { RepetitionTimeInputGridProps } from './RepetitionTimeInputGrid'
import TimeInputs from './TimeInputs'
import {
  allChildrenAreAbsent,
  allChildrenHaveDayOff,
  bindUnboundedTimeRanges,
  emptyTimeRange,
  getCommonTimeRanges,
  hasReservationsForEveryChild
} from './utils'

export default React.memo(function IrregularRepetitionTimeInputGrid({
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
  const [lang] = useLang()

  useEffect(() => {
    if (!selectedRange) return

    const reservations = existingReservations.filter((reservation) =>
      selectedRange.includes(reservation.date)
    )

    updateForm({
      irregularTimes: Object.fromEntries(
        [...selectedRange.dates()].map<
          [string, TimeRanges | 'absent' | 'day-off' | undefined]
        >((rangeDate) => {
          const existingTimes = reservations.find(({ date }) =>
            rangeDate.isEqual(date)
          )

          if (!existingTimes) {
            return [rangeDate.formatIso(), emptyTimeRange]
          }

          if (
            allChildrenHaveDayOff([existingTimes], formData.selectedChildren)
          ) {
            return [rangeDate.formatIso(), 'day-off']
          }

          if (
            allChildrenAreAbsent([existingTimes], formData.selectedChildren)
          ) {
            return [rangeDate.formatIso(), 'absent']
          }

          if (
            !hasReservationsForEveryChild(
              [existingTimes],
              formData.selectedChildren
            )
          ) {
            return [rangeDate.formatIso(), emptyTimeRange]
          }

          const commonTimeRanges = getCommonTimeRanges(
            [existingTimes],
            formData.selectedChildren
          )

          if (commonTimeRanges) {
            return [
              rangeDate.formatIso(),
              bindUnboundedTimeRanges(commonTimeRanges)
            ]
          }

          return [rangeDate.formatIso(), emptyTimeRange]
        })
      )
    })
  }, [
    existingReservations,
    formData.selectedChildren,
    selectedRange,
    updateForm
  ])

  return (
    <>
      {[...selectedRange.dates()].map((date, index) => (
        <Fragment key={`shift-care-${date.formatIso()}`}>
          {index !== 0 && date.getIsoDayOfWeek() === 1 ? <Separator /> : null}
          {index === 0 || date.getIsoDayOfWeek() === 1 ? (
            <Week>
              {i18n.common.datetime.week} {date.getIsoWeek()}
            </Week>
          ) : null}
          {includedDays.includes(date.getIsoDayOfWeek()) && (
            <TimeInputs
              label={<Label>{date.format('EEEEEE d.M.', lang)}</Label>}
              times={
                formData.irregularTimes[date.formatIso()] ?? emptyTimeRange
              }
              updateTimes={(times) =>
                updateForm({
                  irregularTimes: {
                    ...formData.irregularTimes,
                    [date.formatIso()]: times
                  }
                })
              }
              validationErrors={
                validationResult.errors?.irregularTimes?.[date.formatIso()]
              }
              showAllErrors={showAllErrors}
              allowExtraTimeRange={childrenInShiftCare}
              dataQaPrefix={`irregular-${date.formatIso()}`}
              onFocus={(ev) => {
                scrollIntoViewSoftKeyboard(ev.target)
              }}
              showAbsences
            />
          )}
        </Fragment>
      ))}
    </>
  )
})

const Week = styled.div`
  color: ${(p) => p.theme.colors.main.m1};
  font-weight: ${fontWeights.semibold};
  grid-column-start: 1;
  grid-column-end: 4;
`

const Separator = styled.div`
  border-top: 2px dotted ${(p) => p.theme.colors.grayscale.g15};
  margin: ${defaultMargins.s} 0;
  grid-column-start: 1;
  grid-column-end: 4;
`
