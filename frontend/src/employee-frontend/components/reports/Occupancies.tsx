// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'react-router-dom'
import { addDays, isAfter, isWeekend, lastDayOfMonth } from 'date-fns'

import { formatPercentage, formatDecimal } from 'lib-common/utils/number'
import { Loading, Result, Success } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Translations, useTranslation } from '../../state/i18n'
import { OccupancyReportRow } from '../../types/reports'
import {
  getOccupanciesReport,
  OccupancyReportFilters,
  OccupancyReportType
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { DATE_FORMAT_ISO, formatDate } from 'lib-common/date'
import { CareArea } from '../../types/unit'
import { getAreas } from '../../api/daycare'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { FlexRow } from '../common/styled/containers'
import { range } from 'lodash'
import LocalDate from 'lib-common/local-date'

const StyledTd = styled(Td)`
  white-space: nowrap;
`

const Wrapper = styled.div`
  width: 100%;
`

const monthOptions = range(1, 13)
const yearOptions = range(
  LocalDate.today().year + 2,
  LocalDate.today().year - 5,
  -1
)

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

type ValueOnReport = 'percentage' | 'headcount' | 'raw'

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
    if (usedValues === 'raw') {
      const cells = [row.unitName, row.groupName].filter(
        (cell) => cell !== undefined
      ) as string[]
      for (const date of dates) {
        const occupancy = row.occupancies[formatDate(date, DATE_FORMAT_ISO)]
        cells.push(
          typeof occupancy?.sum === 'number'
            ? occupancy.sum.toFixed(2).replace('.', ',')
            : '0'
        )
        cells.push(
          typeof occupancy?.caretakers === 'number'
            ? occupancy.caretakers.toFixed(2).replace('.', ',')
            : '0'
        )
      }
      return cells
    } else {
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
    }
  })
}

function Occupancies() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<OccupancyReportRow[]>>(Success.of([]))
  const [areas, setAreas] = useState<CareArea[]>([])
  const [filters, setFilters] = useState<OccupancyReportFilters>({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    careAreaId: '',
    type: 'UNIT_CONFIRMED'
  })
  const [usedValues, setUsedValues] = useState<ValueOnReport>('percentage')

  useEffect(() => {
    void getAreas().then((res) => res.isSuccess && setAreas(res.value))
  }, [])

  useEffect(() => {
    if (filters.careAreaId == '') return

    setRows(Loading.of())
    void getOccupanciesReport(filters).then(setRows)
  }, [filters])

  const dates = getDisplayDates(filters.year, filters.month, filters.type)
  const displayCells: string[][] = rows
    .map((rs) => getDisplayCells(rs, dates, usedValues))
    .getOrElse([])
  const dateCols = dates.reduce((cols, date) => {
    cols.push(date)
    if (usedValues === 'raw') cols.push(date)
    return cols
  }, [] as Date[])

  const includeGroups = filters.type.startsWith('GROUP_')

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.occupancies.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <Combobox
                items={monthOptions}
                selectedItem={filters.month}
                onChange={(month) => {
                  if (month !== null) {
                    setFilters({ ...filters, month })
                  }
                }}
                getItemLabel={(month) => i18n.datePicker.months[month - 1]}
              />
            </Wrapper>
            <Wrapper>
              <Combobox
                items={yearOptions}
                selectedItem={filters.year}
                onChange={(year) => {
                  if (year !== null) {
                    setFilters({ ...filters, year })
                  }
                }}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.occupancies.filters.type}</FilterLabel>
          <Wrapper>
            <Combobox
              items={[
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
              selectedItem={{
                value: filters.type,
                label: i18n.reports.occupancies.filters.types[filters.type]
              }}
              onChange={(value) => {
                if (value) {
                  setFilters({
                    ...filters,
                    type: value.value as OccupancyReportType
                  })
                }
              }}
              getItemLabel={(item) => item.label}
            />
          </Wrapper>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper>
            <Combobox
              items={areas}
              onChange={(item) => {
                if (item) {
                  setFilters({ ...filters, careAreaId: item.id })
                }
              }}
              selectedItem={
                areas.find((area) => area.id === filters.careAreaId) ?? null
              }
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
              getItemLabel={(item) => item.name}
            />
          </Wrapper>
        </FilterRow>
        <FilterRow>
          <FilterLabel>
            {i18n.reports.occupancies.filters.valueOnReport}
          </FilterLabel>
          <Wrapper>
            <Combobox
              items={[
                {
                  value: 'percentage' as ValueOnReport,
                  label:
                    i18n.reports.occupancies.filters.valuesOnReport.percentage
                },
                {
                  value: 'headcount' as ValueOnReport,
                  label:
                    i18n.reports.occupancies.filters.valuesOnReport.headcount
                },
                {
                  value: 'raw' as ValueOnReport,
                  label: i18n.reports.occupancies.filters.valuesOnReport.raw
                }
              ]}
              selectedItem={{
                value: usedValues,
                label:
                  i18n.reports.occupancies.filters.valuesOnReport[usedValues]
              }}
              onChange={(value) => {
                if (value) {
                  setUsedValues(value.value)
                }
              }}
              getItemLabel={(item) => item.label}
            />
          </Wrapper>
        </FilterRow>

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && filters.careAreaId != '' && (
          <>
            <ReportDownload
              data={[
                [
                  i18n.reports.common.unitName,
                  includeGroups ? i18n.reports.common.groupName : undefined,
                  usedValues !== 'raw'
                    ? i18n.reports.occupancies.average
                    : undefined,
                  ...dateCols.map((date) => formatDate(date, 'dd.MM.'))
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
                  {usedValues !== 'raw' && (
                    <Th>{i18n.reports.occupancies.average}</Th>
                  )}
                  {dateCols.map((date, i) => (
                    <Th key={`${date.toDateString()}:${i}`}>
                      {formatDate(date, 'dd.MM.')}
                    </Th>
                  ))}
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row, rowNum) => (
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
