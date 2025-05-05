// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'

import { useTranslation } from '../../state/i18n'

export const isChangeRetroactive = (
  newRange: DateRange | FiniteDateRange | null,
  prevRange: DateRange | FiniteDateRange | null,
  contentChanged: boolean,
  today: LocalDate
): boolean => {
  if (!newRange) {
    // form is not yet valid anyway
    return false
  }
  const processedEnd = today.withDate(1).subDays(1)

  const newRangeAffectsHistory = newRange.start.isEqualOrBefore(processedEnd)
  if (prevRange === null) {
    // creating new, not editing
    return newRangeAffectsHistory
  }

  const prevRangeAffectsHistory = prevRange.start.isEqualOrBefore(processedEnd)
  const eitherRangeAffectHistory =
    newRangeAffectsHistory || prevRangeAffectsHistory

  if (contentChanged && eitherRangeAffectHistory) {
    return true
  }

  if (!newRange.start.isEqual(prevRange.start) && eitherRangeAffectHistory) {
    return true
  }

  if (newRange.end === null) {
    if (prevRange.end === null) {
      // neither is finite
      return newRange.start !== prevRange.start && eitherRangeAffectHistory
    } else {
      // end date has now been removed
      return prevRange.end.isEqualOrBefore(processedEnd)
    }
  } else {
    if (prevRange.end === null) {
      // end date has now been set
      return newRange.end.isEqualOrBefore(processedEnd)
    } else {
      // both are finite
      if (!newRange.start.isEqual(prevRange.start)) {
        return eitherRangeAffectHistory
      } else if (!newRange.end.isEqual(prevRange.end)) {
        return (
          newRange.end.isEqualOrBefore(processedEnd) ||
          prevRange.end.isEqualOrBefore(processedEnd)
        )
      } else {
        return false
      }
    }
  }
}

const RetroactiveConfirmation = React.memo(function RetroactiveConfirmation({
  confirmed,
  setConfirmed
}: {
  confirmed: boolean
  setConfirmed: (confirmed: boolean) => void
}) {
  const { i18n } = useTranslation()
  return (
    <AlertBox
      noMargin
      wide
      title={i18n.common.retroactiveConfirmation.title}
      message={
        <Checkbox
          label={i18n.common.retroactiveConfirmation.checkboxLabel}
          checked={confirmed}
          onChange={setConfirmed}
          data-qa="confirm-retroactive"
        />
      }
    />
  )
})

export default RetroactiveConfirmation
