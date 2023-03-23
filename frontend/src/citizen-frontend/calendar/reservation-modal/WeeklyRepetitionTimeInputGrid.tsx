// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm, useFormElems, useFormField } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { LabelLike } from 'lib-components/typography'

import TimeInputs from './TimeInputs'
import { weekDay, weeklyTimes } from './form'

export interface WeeklyRepetitionTimeInputGridProps {
  bind: BoundForm<typeof weeklyTimes>
  childrenInShiftCare: boolean
  includedDays: number[]
  showAllErrors: boolean
}

export default React.memo(function WeeklyRepetitionTimeInputGrid({
  bind,
  showAllErrors,
  childrenInShiftCare,
  includedDays
}: WeeklyRepetitionTimeInputGridProps) {
  const weekDays = useFormElems(bind)
  return (
    <>
      {weekDays.map((weekDay, index) =>
        includedDays.includes(index + 1) ? (
          <DayInputs
            key={`day-${index}`}
            bind={weekDay}
            index={index}
            showAllErrors={showAllErrors}
            childrenInShiftCare={childrenInShiftCare}
          />
        ) : null
      )}
    </>
  )
})

const DayInputs = React.memo(function Foo({
  bind,
  index,
  showAllErrors,
  childrenInShiftCare
}: {
  bind: BoundForm<typeof weekDay>
  index: number
  showAllErrors: boolean
  childrenInShiftCare: boolean
}) {
  const i18n = useTranslation()
  const mode = useFormField(bind, 'mode')
  const day = useFormField(bind, 'day')
  const present = useFormField(day, 'present')
  const times = useFormField(day, 'times')
  return (
    <TimeInputs
      label={<LabelLike>{i18n.common.datetime.weekdaysShort[index]}</LabelLike>}
      mode={mode.value()}
      bindPresent={present}
      bindTimes={times}
      showAllErrors={showAllErrors}
      allowExtraTimeRange={childrenInShiftCare}
      dataQaPrefix={`weekly-${index}`}
      onFocus={(ev) => {
        scrollIntoViewSoftKeyboard(ev.target)
      }}
    />
  )
})
