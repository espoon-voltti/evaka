// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { Label } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle, faTrash } from 'lib-icons'

import { useLang, useTranslation } from '../../localization'

export type HolidayErrorKey = HolidayRowState['key'] | 'overlaps'
type ArbitraryId = string

export interface HolidayRowState {
  key: ArbitraryId
  start: string
  end: string
}

interface Props {
  period: FiniteDateRange
  selectedFreePeriod: FiniteDateRange | null
  rows: HolidayRowState[]
  updateHoliday: (row: HolidayRowState) => void
  addRow: () => void
  removeRow: (key: ArbitraryId) => void
  errors: HolidayErrorKey[]
}

export const HolidaySelector = React.memo(function HolidaySelector({
  period,
  selectedFreePeriod,
  rows,
  updateHoliday,
  removeRow,
  addRow,
  errors
}: Props) {
  const [lang] = useLang()
  const i18n = useTranslation()

  const isValidDate = useCallback(
    (date: LocalDate) =>
      period.includes(date) && !selectedFreePeriod?.includes(date),
    [selectedFreePeriod, period]
  )

  return (
    <div>
      <Label>{i18n.calendar.holidayModal.childOnHoliday}</Label>

      <FixedSpaceColumn>
        {rows.map((row, index) => (
          <FixedSpaceRow key={row.key} alignItems="center">
            <DatePicker
              date={row.start}
              isValidDate={isValidDate}
              initialMonth={period.start}
              locale={lang}
              onChange={(start: string) => updateHoliday({ ...row, start })}
            />
            <span> – </span>
            <DatePicker
              date={row.end}
              isValidDate={isValidDate}
              initialMonth={period.start}
              locale={lang}
              onChange={(end: string) => updateHoliday({ ...row, end })}
            />
            {index > 0 && (
              <IconButton icon={faTrash} onClick={() => removeRow(row.key)} />
            )}
            {errors.find((e) => e === row.key) && (
              <FontAwesomeIcon
                icon={fasExclamationTriangle}
                size="1x"
                color={colors.status.warning}
              />
            )}
          </FixedSpaceRow>
        ))}
        <AddButton
          onClick={addRow}
          text={i18n.calendar.holidayModal.addTimePeriod}
        />
      </FixedSpaceColumn>
    </div>
  )
})
