// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useTranslation } from 'citizen-frontend/localization'
import { BoundForm } from 'lib-common/form/hooks'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { Label } from 'lib-components/typography'

import { Times } from './TimeInputs'
import { times } from './form'

export interface DailyRepetitionTimeInputGridProps {
  bind: BoundForm<typeof times>
  childrenInShiftCare: boolean
  includedDays: number[]
  showAllErrors: boolean
}

export default React.memo(function DailyRepetitionTimeInputGrid({
  bind,
  showAllErrors,
  childrenInShiftCare,
  includedDays
}: DailyRepetitionTimeInputGridProps) {
  const i18n = useTranslation()

  const label = (
    <Label>{`${i18n.common.datetime.weekdaysShort[includedDays[0] - 1]}-${
      i18n.common.datetime.weekdaysShort[
        includedDays[includedDays.length - 1] - 1
      ]
    }`}</Label>
  )

  return (
    <Times
      bind={bind}
      label={label}
      showAllErrors={showAllErrors}
      allowExtraTimeRange={childrenInShiftCare}
      dataQaPrefix="daily"
      onFocus={(ev) => {
        scrollIntoViewSoftKeyboard(ev.target)
      }}
    />
  )
})
