// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm, useFormElems } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { LabelLike } from 'lib-components/typography'

import { Day } from './TimeInputs'
import { weeklyTimes } from './form'

export interface WeeklyRepetitionTimeInputGridProps {
  bind: BoundForm<typeof weeklyTimes>
  anyChildInShiftCare: boolean
  showAllErrors: boolean
}

export default React.memo(function WeeklyRepetitionTimeInputGrid({
  bind,
  showAllErrors,
  anyChildInShiftCare
}: WeeklyRepetitionTimeInputGridProps) {
  const i18n = useTranslation()
  const weekDays = useFormElems(bind)
  return (
    <>
      {weekDays.map((weekDay, index) => (
        <Day
          key={`day-${index}`}
          bind={weekDay}
          label={
            <LabelLike>{i18n.common.datetime.weekdaysShort[index]}</LabelLike>
          }
          showAllErrors={showAllErrors}
          allowExtraTimeRange={anyChildInShiftCare}
          dataQaPrefix={`weekly-${index}`}
          onFocus={(ev) => {
            scrollIntoViewSoftKeyboard(ev.target)
          }}
        />
      ))}
    </>
  )
})
