// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import { DatePickerDeprecated } from '@evaka/lib-components/src/molecules/DatePickerDeprecated'
import styled from 'styled-components'
import Chip, { Chips } from 'components/common/Chip'
import { UnitFilters } from 'utils/UnitFilters'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'

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
            dataQa="unit-filter-start-date"
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

      <Chips>
        <Chip
          text={i18n.unit.filters.periods.day}
          active={period === '1 day'}
          onClick={() => setFilters(filters.withPeriod('1 day'))}
          dataQa="unit-filter-period-1-day"
        />
        <Chip
          text={i18n.unit.filters.periods.threeMonths}
          active={period === '3 months'}
          onClick={() => setFilters(filters.withPeriod('3 months'))}
          dataQa="unit-filter-period-3-months"
        />
        <Chip
          text={i18n.unit.filters.periods.sixMonths}
          active={period === '6 months'}
          onClick={() => setFilters(filters.withPeriod('6 months'))}
          dataQa="unit-filter-period-6-months"
        />
        <Chip
          text={i18n.unit.filters.periods.year}
          active={period === '1 year'}
          onClick={() => setFilters(filters.withPeriod('1 year'))}
          dataQa="unit-filter-period-1-year"
        />
      </Chips>
    </FixedSpaceRow>
  )
})
