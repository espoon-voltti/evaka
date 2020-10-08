// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { reactSelectStyles } from '~components/shared/utils'
import { Translations, useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result, Success } from '~api'
import { OccupancyReportRow } from '~types/reports'
import {
  getOccupanciesReport,
  OccupancyReportFilters,
  OccupancyReportType
} from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { addDays, isAfter, isWeekend, lastDayOfMonth } from 'date-fns'
import { formatDate } from '~utils/date'
import { DATE_FORMAT_ISO } from '~constants'
import { formatPercentage, formatDecimal } from '~components/utils'
import { SelectOptionProps } from '~components/common/Select'
import { fi } from 'date-fns/locale'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '~components/reports/common'
import { FlexRow } from 'components/common/styled/containers'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const Wrapper = styled.div`
  width: 100%;
`

function monthOptions(): SelectOptionProps[] {
  const monthOptions = []
  for (let i = 1; i <= 12; i++) {
    monthOptions.push({
      value: i.toString(),
      label: String(fi.localize?.month(i - 1))
    })
  }
  return monthOptions
}

function yearOptions(): SelectOptionProps[] {
  const currentYear = new Date().getFullYear()
  const yearOptions = []
  for (let year = currentYear + 2; year > currentYear - 5; year--) {
    yearOptions.push({
      value: year.toString(),
      label: year.toString()
    })
  }
  return yearOptions
}

function getDisplayDates(
  year: number,
  month: number,
  type: OccupancyReportType
) {
  const fromDate = new Date(year, month - 1, 1)
  let toDate = lastDayOfMonth(fromDate)
  if (type.includes('REALIZED') && isAfter(toDate, new Date()))
    toDate = new Date()
  const dates: Date[] = []
  for (let date = fromDate; date <= toDate; date = addDays(date, 1)) {
    if (!isWeekend(date)) dates.push(date)
  }
  return dates
}

function getFilename(
  i18n: Translations,
  year: number,
  month: number,
  type: OccupancyReportType,
  careAreaName: string
) {
  const prefix = i18n.reports.occupancies.filters.types[type]
  const time = formatDate(new Date(year, month - 1, 1), 'yyyy-MM')
  return `${prefix}-${time}-${careAreaName}.csv`.replace(/ /g, '_')
}

type ValueOnReport = 'percentage' | 'headcount'

function getOccupancyAverage(
  row: OccupancyReportRow,
  dates: Date[]
): number | null {
  const capacitySum = dates.reduce((sum, date) => {
    return sum + (row.occupancies[formatDate(date, DATE_FORMAT_ISO)]?.sum ?? 0)
  }, 0)

  const caretakersSum = dates.reduce((sum, date) => {
    return (
      sum +
      (row.occupancies[formatDate(date, DATE_FORMAT_ISO)]?.caretakers ?? 0)
    )
  }, 0)

  if (caretakersSum > 0) {
    return (100 * capacitySum) / (7 * caretakersSum)
  } else {
    return null
  }
}

function getHeadCountAverage(
  row: OccupancyReportRow,
  dates: Date[]
): number | null {
  const headCountSum = dates.reduce((sum, date) => {
    return (
      sum + (row.occupancies[formatDate(date, DATE_FORMAT_ISO)]?.headcount ?? 0)
    )
  }, 0)

  if (dates.length > 0) {
    return headCountSum / dates.length
  } else {
    return null
  }
}

function getDisplayCells(
  reportRows: OccupancyReportRow[],
  dates: Date[],
  usedValues: ValueOnReport
): string[][] {
  return reportRows.map((row) => {
    const average =
      usedValues === 'headcount'
        ? getHeadCountAverage(row, dates)
        : getOccupancyAverage(row, dates)

    const roundedAverage =
      average !== null ? Math.round(10 * average) / 10 : undefined

    return [
      row.unitName,
      row.groupName,

      // average
      (usedValues === 'percentage'
        ? formatPercentage(roundedAverage)
        : formatDecimal(roundedAverage)) ?? '',

      // daily values
      ...dates.map((date) => {
        const occupancy = row.occupancies[formatDate(date, DATE_FORMAT_ISO)]

        if (usedValues === 'percentage') {
          return typeof occupancy?.percentage === 'number'
            ? formatPercentage(occupancy.percentage)
            : ''
        } else {
          return occupancy?.[usedValues] ?? ''
        }
      })
    ].filter((cell) => cell !== undefined) as string[]
  })
}

function Occupancies() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<OccupancyReportRow[]>>(Success([]))
  const [areas, setAreas] = useState<CareArea[]>([])
  const [filters, setFilters] = useState<OccupancyReportFilters>({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    careAreaId: '',
    type: 'UNIT_CONFIRMED'
  })
  const [usedValues, setUsedValues] = useState<ValueOnReport>('percentage')

  useEffect(() => {
    void getAreas().then((res) => isSuccess(res) && setAreas(res.data))
  }, [])

  useEffect(() => {
    if (filters.careAreaId == '') return

    setRows(Loading())
    void getOccupanciesReport(filters).then(setRows)
  }, [filters])

  const dates = getDisplayDates(filters.year, filters.month, filters.type)
  const displayCells: string[][] = isSuccess(rows)
    ? getDisplayCells(rows.data, dates, usedValues)
    : []

  const includeGroups = filters.type.startsWith('GROUP_')

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.occupancies.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <ReactSelect
                options={monthOptions()}
                onChange={(value) => {
                  if (value && 'value' in value) {
                    const month = parseInt(value.value)
                    setFilters({ ...filters, month })
                  }
                }}
                styles={reactSelectStyles}
              />
            </Wrapper>
            <Wrapper>
              <ReactSelect
                options={yearOptions()}
                onChange={(value) => {
                  if (value && 'value' in value) {
                    const year = parseInt(value.value)
                    setFilters({ ...filters, year })
                  }
                }}
                styles={reactSelectStyles}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.occupancies.filters.type}</FilterLabel>
          <Wrapper>
            <ReactSelect
              options={[
                {
                  value: 'UNIT_CONFIRMED',
                  label: i18n.reports.occupancies.filters.types.UNIT_CONFIRMED
                },
                {
                  value: 'UNIT_PLANNED',
                  label: i18n.reports.occupancies.filters.types.UNIT_PLANNED
                },
                {
                  value: 'UNIT_REALIZED',
                  label: i18n.reports.occupancies.filters.types.UNIT_REALIZED
                },
                {
                  value: 'GROUP_CONFIRMED',
                  label: i18n.reports.occupancies.filters.types.GROUP_CONFIRMED
                },
                {
                  value: 'GROUP_REALIZED',
                  label: i18n.reports.occupancies.filters.types.GROUP_REALIZED
                }
              ]}
              onChange={(value) => {
                if (value && 'value' in value) {
                  setFilters({
                    ...filters,
                    type: value.value as OccupancyReportType
                  })
                }
              }}
              styles={reactSelectStyles}
            />
          </Wrapper>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <ReactSelect
              options={[
                ...areas.map((area) => ({ value: area.id, label: area.name }))
              ]}
              onChange={(value) => {
                if (value && 'value' in value) {
                  setFilters({ ...filters, careAreaId: value.value })
                }
              }}
              styles={reactSelectStyles}
            />
          </Wrapper>
        </FilterRow>
        <FilterRow>
          <FilterLabel>
            {i18n.reports.occupancies.filters.valueOnReport}
          </FilterLabel>
          <Wrapper>
            <ReactSelect
              options={[
                {
                  value: 'percentage',
                  label:
                    i18n.reports.occupancies.filters.valuesOnReport.percentage
                },
                {
                  value: 'headcount',
                  label:
                    i18n.reports.occupancies.filters.valuesOnReport.headcount
                }
              ]}
              onChange={(value) => {
                if (value && 'value' in value) {
                  setUsedValues(
                    value.value === 'headcount' ? 'headcount' : 'percentage'
                  )
                }
              }}
              styles={reactSelectStyles}
            />
          </Wrapper>
        </FilterRow>
        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && filters.careAreaId != '' && (
          <>
            <ReportDownload
              data={[
                [
                  i18n.reports.common.unitName,
                  includeGroups ? i18n.reports.common.groupName : undefined,
                  i18n.reports.occupancies.average,
                  ...dates.map((date) => formatDate(date, 'dd.MM.'))
                ].filter((label) => label !== undefined),
                ...displayCells
              ]}
              filename={getFilename(
                i18n,
                filters.year,
                filters.month,
                filters.type,
                areas.find((area) => area.id == filters.careAreaId)?.name ?? ''
              )}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.unitName}</Th>
                  {includeGroups && <Th>{i18n.reports.common.groupName}</Th>}
                  <Th>{i18n.reports.occupancies.average}</Th>
                  {dates.map((date) => (
                    <Th key={date.toDateString()}>
                      {formatDate(date, 'dd.MM.')}
                    </Th>
                  ))}
                </Tr>
              </Thead>
              <Tbody>
                {rows.data.map((row, rowNum) => (
                  <Tr key={row.unitId}>
                    <StyledTd>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </StyledTd>
                    {displayCells[rowNum].slice(1).map((cell, colNum) => (
                      <StyledTd key={colNum}>{cell}</StyledTd>
                    ))}
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default Occupancies
