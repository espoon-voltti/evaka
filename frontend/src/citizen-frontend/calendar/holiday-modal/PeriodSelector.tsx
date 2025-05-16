// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import Select from 'lib-components/atoms/dropdowns/Select'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../localization'

interface HolidayOption {
  name: string
  period: FiniteDateRange | null
}

interface Props {
  label: string
  options: FiniteDateRange[]
  value: FiniteDateRange | null
  onSelectPeriod: (selection: FiniteDateRange | null) => void
}

export const PeriodSelector = React.memo(function PeriodSelector({
  label,
  options,
  value,
  onSelectPeriod
}: Props) {
  const i18n = useTranslation()

  const emptySelection = useMemo(
    () => ({
      name: i18n.calendar.holidayModal.emptySelection,
      period: null
    }),
    [i18n]
  )

  const items = useMemo<HolidayOption[]>(
    () => [
      emptySelection,
      ...options.map((period) => ({
        name: period.format(),
        period
      }))
    ],
    [options, emptySelection]
  )

  return (
    <div>
      <Label>{label}</Label>
      <Select
        items={items}
        selectedItem={
          items.find(({ period }) =>
            value === null ? period === null : period?.isEqual(value)
          ) ?? emptySelection
        }
        onChange={(item) => onSelectPeriod(item?.period ?? null)}
        getItemValue={({ name }) => name}
        getItemLabel={({ name }) => name}
        data-qa="period-select"
      />
    </div>
  )
})
