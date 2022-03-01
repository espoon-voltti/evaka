// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
import { faTrash } from 'lib-icons'

import { useLang, useTranslation } from '../../localization'

type ArbitraryId = string

export interface HolidayRowState {
  key: ArbitraryId
  start: string
  end: string
  parsedStart: LocalDate | null
  parsedEnd: LocalDate | null
  errorKey: string | undefined
}

interface Props {
  period: FiniteDateRange
  rows: HolidayRowState[]
  updateHoliday: (row: HolidayRowState) => void
  addRow: () => void
  removeRow: (key: ArbitraryId) => void
}

export const HolidaySelector = React.memo(function HolidaySelector({
  period,
  rows,
  updateHoliday,
  removeRow,
  addRow
}: Props) {
  const [lang] = useLang()
  const i18n = useTranslation()

  const isValidDate = useCallback(
    (date: LocalDate) => period.includes(date),
    [period]
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
              hideErrorsBeforeTouched
              onChange={(start: string) =>
                updateHoliday({
                  ...row,
                  start,
                  parsedStart: LocalDate.parseFiOrNull(start),
                  errorKey: undefined
                })
              }
            />
            <span> – </span>
            <DatePicker
              date={row.end}
              isValidDate={isValidDate}
              initialMonth={period.start}
              locale={lang}
              hideErrorsBeforeTouched
              onChange={(end: string) =>
                updateHoliday({
                  ...row,
                  end,
                  parsedEnd: LocalDate.parseFiOrNull(end),
                  errorKey: undefined
                })
              }
            />
            {index > 0 && (
              <IconButton icon={faTrash} onClick={() => removeRow(row.key)} />
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
