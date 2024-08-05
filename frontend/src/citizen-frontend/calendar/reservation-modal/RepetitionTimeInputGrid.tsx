// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { BoundForm, useFormUnion } from 'lib-common/form/hooks'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { useTranslation } from '../../localization'

import DailyRepetitionTimeInputGrid from './DailyRepetitionTimeInputGrid'
import IrregularRepetitionTimeInputGrid from './IrregularRepetitionTimeInputGrid'
import WeeklyRepetitionTimeInputGrid from './WeeklyRepetitionTimeInputGrid'
import { type timesUnion } from './form'

export interface RepetitionTimeInputGridProps {
  bind: BoundForm<typeof timesUnion>
  showAllErrors: boolean
}

export default React.memo(function RepetitionTimeInputGrid({
  bind,
  ...props
}: RepetitionTimeInputGridProps) {
  const i18n = useTranslation()
  const { branch, form } = useFormUnion(bind)
  switch (branch) {
    case 'dailyTimes':
      return (
        <FixedSpaceColumn>
          <DailyRepetitionTimeInputGrid bind={form} {...props} />
        </FixedSpaceColumn>
      )
    case 'weeklyTimes':
      return (
        <FixedSpaceColumn>
          <WeeklyRepetitionTimeInputGrid bind={form} {...props} />
        </FixedSpaceColumn>
      )
    case 'irregularTimes':
      return (
        <FixedSpaceColumn>
          <IrregularRepetitionTimeInputGrid bind={form} {...props} />
        </FixedSpaceColumn>
      )
    case 'notInitialized':
      return (
        <FixedSpaceColumn>
          <div>{i18n.calendar.reservationModal.noReservableDays}</div>
        </FixedSpaceColumn>
      )
  }
})
