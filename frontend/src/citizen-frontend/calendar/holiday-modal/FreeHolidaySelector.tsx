// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { FreeAbsencePeriod } from 'lib-common/generated/api-types/holidayperiod'
import Select from 'lib-components/atoms/dropdowns/Select'
import { Label } from 'lib-components/typography'

import { useLang } from '../../localization'

interface HolidayOption {
  name: string
  period: FiniteDateRange | null
}
const emptySelection = {
  name: '',
  period: null
} as const

interface Props {
  freeAbsencePeriod: FreeAbsencePeriod
  value: FiniteDateRange | null
  onSelectPeriod: (selection: FiniteDateRange | null) => void
}

export const FreeHolidaySelector = React.memo(function FreeHolidaySelector({
  freeAbsencePeriod,
  value,
  onSelectPeriod
}: Props) {
  const [lang] = useLang()

  const options = useMemo<HolidayOption[]>(
    () => [
      emptySelection,
      ...freeAbsencePeriod.periodOptions.map((period) => ({
        name: period.format(),
        period
      }))
    ],
    [freeAbsencePeriod]
  )

  return (
    <div>
      <Label>{freeAbsencePeriod.periodOptionLabel[lang]}</Label>
      <Select
        items={options}
        selectedItem={
          options.find(({ period }) =>
            value === null ? period === null : period?.isEqual(value)
          ) ?? emptySelection
        }
        onChange={(item) => onSelectPeriod(item?.period ?? null)}
        getItemValue={({ name }) => name}
        getItemLabel={({ name }) => name}
        data-qa="free-period-select"
      />
    </div>
  )
})
