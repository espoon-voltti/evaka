// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import type { BoundForm } from 'lib-common/form/hooks'
import { useFormElems, useFormField } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { LabelLike } from 'lib-components/typography'

import { Day } from './TimeInputs'
import type { weekDay, weeklyTimes } from './form'

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
  const weekDays = useFormElems(bind)
  return (
    <>
      {weekDays.map((form) => (
        <WeekDay
          key={form.state.weekDay}
          bind={form}
          showAllErrors={showAllErrors}
          anyChildInShiftCare={anyChildInShiftCare}
        />
      ))}
    </>
  )
})

const WeekDay = React.memo(function WeekDay({
  bind,
  showAllErrors,
  anyChildInShiftCare
}: {
  bind: BoundForm<typeof weekDay>
  showAllErrors: boolean
  anyChildInShiftCare: boolean
}) {
  const i18n = useTranslation()

  const day = useFormField(bind, 'day')
  const weekDay = bind.state.weekDay

  return (
    <Day
      bind={day}
      label={
        <LabelLike>{i18n.common.datetime.weekdaysShort[weekDay - 1]}</LabelLike>
      }
      showAllErrors={showAllErrors}
      allowExtraTimeRange={anyChildInShiftCare}
      dataQaPrefix={`weekly-${weekDay - 1}`}
      onFocus={(ev) => {
        scrollIntoViewSoftKeyboard(ev.target)
      }}
    />
  )
})
