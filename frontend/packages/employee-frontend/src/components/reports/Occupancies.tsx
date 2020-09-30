// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import {
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
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
import styled from 'styled-components'
import { addDays, isAfter, isWeekend, lastDayOfMonth } from 'date-fns'
import { formatDate } from '~utils/date'
import { DATE_FORMAT_ISO } from '~constants'
import { formatPercentage, formatDecimal } from '~components/utils'
import SelectWithIcon, { SelectOptionProps } from '~components/common/Select'
import { fi } from 'date-fns/locale'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '~components/reports/common'
import { Link } from 'react-router-dom'
import { FlexRow } from 'components/common/styled/containers'

const StyledTd = styled(Table.Td)`
  white-space: nowrap;
`

function monthOptions(): SelectOptionProps[] {
  const monthOptions = []
  for (let i = 1; i <= 12; i++) {
    monthOptions.push({
      id: i.toString(),
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
      id: year.toString(),
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
            <SelectWithIcon
              options={monthOptions()}
              value={filters.month.toString()}
              onChange={(e) => {
                const month = parseInt(e.target.value)
                setFilters({ ...filters, month })
              }}
              fullWidth
            />
            <SelectWithIcon
              options={yearOptions()}
              value={filters.year.toString()}
              onChange={(e) => {
                const year = parseInt(e.target.value)
                setFilters({ ...filters, year })
              }}
              fullWidth
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.occupancies.filters.type}</FilterLabel>
          <SelectWithIcon
            options={[
              {
                id: 'UNIT_CONFIRMED',
                label: i18n.reports.occupancies.filters.types.UNIT_CONFIRMED
              },
              {
                id: 'UNIT_PLANNED',
                label: i18n.reports.occupancies.filters.types.UNIT_PLANNED
              },
              {
                id: 'UNIT_REALIZED',
                label: i18n.reports.occupancies.filters.types.UNIT_REALIZED
              },
              {
                id: 'GROUP_CONFIRMED',
                label: i18n.reports.occupancies.filters.types.GROUP_CONFIRMED
              },
              {
                id: 'GROUP_REALIZED',
                label: i18n.reports.occupancies.filters.types.GROUP_REALIZED
              }
            ]}
            value={filters.type}
            onChange={(e) =>
              setFilters({
                ...filters,
                type: e.target.value as OccupancyReportType
              })
            }
            fullWidth
          />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <SelectWithIcon
            options={[
              {
                id: '',
                label: i18n.reports.occupancies.filters.areaPlaceholder
              },
              ...areas.map((area) => ({ id: area.id, label: area.name }))
            ]}
            value={filters.careAreaId}
            onChange={(e) =>
              setFilters({ ...filters, careAreaId: e.target.value })
            }
            fullWidth
          />
        </FilterRow>
        <FilterRow>
          <FilterLabel>
            {i18n.reports.occupancies.filters.valueOnReport}
          </FilterLabel>
          <SelectWithIcon
            options={[
              {
                id: 'percentage',
                label:
                  i18n.reports.occupancies.filters.valuesOnReport.percentage
              },
              {
                id: 'headcount',
                label: i18n.reports.occupancies.filters.valuesOnReport.headcount
              }
            ]}
            value={usedValues}
            onChange={(e) =>
              setUsedValues(
                e.target.value === 'headcount' ? 'headcount' : 'percentage'
              )
            }
            fullWidth
          />
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
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  {includeGroups && (
                    <Table.Th>{i18n.reports.common.groupName}</Table.Th>
                  )}
                  <Table.Th>{i18n.reports.occupancies.average}</Table.Th>
                  {dates.map((date) => (
                    <Table.Th key={date.toDateString()}>
                      {formatDate(date, 'dd.MM.')}
                    </Table.Th>
                  ))}
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {rows.data.map((row, rowNum) => (
                  <Table.Row key={row.unitId}>
                    <StyledTd>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </StyledTd>
                    {displayCells[rowNum].slice(1).map((cell, colNum) => (
                      <StyledTd key={colNum}>{cell}</StyledTd>
                    ))}
                  </Table.Row>
                ))}
              </Table.Body>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default Occupancies
