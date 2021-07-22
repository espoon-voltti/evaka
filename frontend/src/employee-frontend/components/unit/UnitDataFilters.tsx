// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import LocalDate from 'lib-common/local-date'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { useTranslation } from '../../state/i18n'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import styled from 'styled-components'
import { UnitFilters } from '../../utils/UnitFilters'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

const DatePickersContainer = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 17em;
  > * {
    margin-right: 8px;
  }
`

type Props = {
  canEdit: boolean
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
}

export default React.memo(function UnitDataFilters({
  canEdit,
  filters,
  setFilters
}: Props) {
  const { i18n } = useTranslation()
  const { startDate, endDate, period } = filters

  return (
    <FixedSpaceRow>
      <DatePickersContainer>
        {canEdit ? (
          <DatePickerDeprecated
            data-qa="unit-filter-start-date"
            date={startDate}
            onChange={(date) => setFilters(filters.withStartDate(date))}
            type="half-width"
            minDate={LocalDate.of(2000, 1, 1)}
            options={{
              todayButton: i18n.common.today
            }}
          />
        ) : (
          <div>{startDate.format()}</div>
        )}
        <div>-</div>
        <div>{endDate.format()}</div>
      </DatePickersContainer>

      <FixedSpaceRow spacing="xs">
        <SelectionChip
          text={i18n.unit.filters.periods.day}
          selected={period === '1 day'}
          onChange={(selected) =>
            selected ? setFilters(filters.withPeriod('1 day')) : undefined
          }
          data-qa="unit-filter-period-1-day"
        />
        <SelectionChip
          text={i18n.unit.filters.periods.threeMonths}
          selected={period === '3 months'}
          onChange={(selected) =>
            selected ? setFilters(filters.withPeriod('3 months')) : undefined
          }
          data-qa="unit-filter-period-3-months"
        />
        <SelectionChip
          text={i18n.unit.filters.periods.sixMonths}
          selected={period === '6 months'}
          onChange={(selected) =>
            selected ? setFilters(filters.withPeriod('6 months')) : undefined
          }
          data-qa="unit-filter-period-6-months"
        />
        <SelectionChip
          text={i18n.unit.filters.periods.year}
          selected={period === '1 year'}
          onChange={(selected) =>
            selected ? setFilters(filters.withPeriod('1 year')) : undefined
          }
          data-qa="unit-filter-period-1-year"
        />
      </FixedSpaceRow>
    </FixedSpaceRow>
  )
})
