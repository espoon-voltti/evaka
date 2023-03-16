// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { BoundForm, useFormField } from 'lib-common/form/hooks'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import DailyRepetitionTimeInputGrid from './DailyRepetitionTimeInputGrid'
import IrregularRepetitionTimeInputGrid from './IrregularRepetitionTimeInputGrid'
import WeeklyRepetitionTimeInputGrid from './WeeklyRepetitionTimeInputGrid'
import { reservationForm } from './form'

export interface RepetitionTimeInputGridProps {
  bind: BoundForm<typeof reservationForm>
  childrenInShiftCare: boolean
  includedDays: number[]
  showAllErrors: boolean
}

export default React.memo(function RepetitionTimeInputGrid({
  bind,
  ...props
}: RepetitionTimeInputGridProps) {
  const repetition = useFormField(bind, 'repetition')
  const dailyTimes = useFormField(bind, 'dailyTimes')
  const weeklyTimes = useFormField(bind, 'weeklyTimes')
  const irregularTimes = useFormField(bind, 'irregularTimes')

  switch (repetition.value()) {
    case 'DAILY':
      return (
        <FixedSpaceColumn>
          <DailyRepetitionTimeInputGrid bind={dailyTimes} {...props} />
        </FixedSpaceColumn>
      )
    case 'WEEKLY':
      return (
        <FixedSpaceColumn>
          <WeeklyRepetitionTimeInputGrid bind={weeklyTimes} {...props} />
        </FixedSpaceColumn>
      )
    case 'IRREGULAR':
      return (
        <FixedSpaceColumn>
          <IrregularRepetitionTimeInputGrid bind={irregularTimes} {...props} />
        </FixedSpaceColumn>
      )
  }
})
