// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { DailyReservationData } from 'lib-common/generated/api-types/reservations'
import {
  Repetition,
  ReservationFormDataForValidation,
  ValidationResult
} from 'lib-common/reservations'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import DailyRepetitionTimeInputGrid from './DailyRepetitionTimeInputGrid'
import IrregularRepetitionTimeInputGrid from './IrregularRepetitionTimeInputGrid'
import WeeklyRepetitionTimeInputGrid from './WeeklyRepetitionTimeInputGrid'

export interface RepetitionTimeInputGridProps {
  formData: ReservationFormDataForValidation
  childrenInShiftCare: boolean
  includedDays: number[]
  updateForm: (updated: Partial<ReservationFormDataForValidation>) => void
  showAllErrors: boolean
  existingReservations: DailyReservationData[]
  validationResult: ValidationResult
  selectedRange: FiniteDateRange
}

export default React.memo(function RepetitionTimeInputGrid({
  repetition,
  ...props
}: RepetitionTimeInputGridProps & { repetition: Repetition }) {
  switch (repetition) {
    case 'DAILY':
      return (
        <FixedSpaceColumn>
          <DailyRepetitionTimeInputGrid {...props} />
        </FixedSpaceColumn>
      )
    case 'WEEKLY':
      return (
        <FixedSpaceColumn>
          <WeeklyRepetitionTimeInputGrid {...props} />
        </FixedSpaceColumn>
      )
    case 'IRREGULAR':
      return (
        <FixedSpaceColumn>
          <IrregularRepetitionTimeInputGrid {...props} />
        </FixedSpaceColumn>
      )
  }
})
