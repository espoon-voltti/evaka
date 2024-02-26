// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { addDays, isAfter, isWeekend, lastDayOfMonth } from 'date-fns'
import mapValues from 'lodash/mapValues'
import range from 'lodash/range'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { combine } from 'lib-common/api'
import { formatDate } from 'lib-common/date'
import { careTypes } from 'lib-common/generated/api-types/daycare'
import { OccupancyType } from 'lib-common/generated/api-types/occupancy'
import {
  OccupancyGroupReportResultRow,
  OccupancyUnitReportResultRow
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'
import { mockNow } from 'lib-common/utils/helpers'
import { formatPercentage, formatDecimal } from 'lib-common/utils/number'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Tfoot, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import { unitProviderTypes } from 'lib-customizations/employee'
import { faChevronDown, faChevronUp } from 'lib-icons'

import ReportDownload from '../../components/reports/ReportDownload'
import {
  getOccupancyGroupReport,
  getOccupancyUnitReport
} from '../../generated/api-clients/reports'
import { Translations, useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { areaQuery } from '../unit/queries'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { occupanciesReportQuery } from './queries'

type DisplayMode = 'UNITS' | 'GROUPS'
export type OccupancyReportFilters =
  | (Arg0<typeof getOccupancyUnitReport> & { display: 'UNITS' })
  | (Arg0<typeof getOccupancyGroupReport> & { display: 'GROUPS' })

type OccupancyReportRow =
  | OccupancyUnitReportResultRow
  | OccupancyGroupReportResultRow

function isGroupRow(
  row: OccupancyReportRow
): row is OccupancyGroupReportResultRow {
  return 'groupId' in row
}

const StyledTfoot = styled(Tfoot)`
  background-color: ${(props) => props.theme.colors.grayscale.g4};
`

const StyledTd = styled(Td)<{ borderEdge?: 'left' | 'right' }>`
  white-space: nowrap;

  ${(props) =>
    props.borderEdge === 'left' &&
    css`
      border-left: 1px solid ${(props) => props.theme.colors.grayscale.g15};
    `}
  ${(props) =>
    props.borderEdge === 'right' &&
    css`
      border-right: 1px solid ${(props) => props.theme.colors.grayscale.g15};
    `}
`

const AccordionIcon = styled(FontAwesomeIcon)`
  cursor: pointer;
  color: ${(p) => p.theme.colors.grayscale.g70};
  padding-right: 1em;
`

const Wrapper = styled.div`
  width: 100%;
`

const monthOptions = range(1, 13)
const yearOptions = range(
  LocalDate.todayInSystemTz().year + 2,
  LocalDate.todayInSystemTz().year - 5,
  -1
)

function getDisplayDates(year: number, month: number, type: OccupancyType) {
  const fromDate = new Date(year, month - 1, 1)
  const now = mockNow() ?? new Date()
  let toDate = lastDayOfMonth(fromDate)
  if (type === 'REALIZED' && isAfter(toDate, now)) toDate = now
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
  display: DisplayMode,
  type: OccupancyType,
  careAreaName: string
) {
  const prefix = i18n.reports.occupancies.filters.types[display][type]
  const time = LocalDate.of(year, month, 1).formatExotic('yyyy-MM')
  return `${prefix}-${time}-${careAreaName}.csv`.replace(/ /g, '_')
}

const toOccupancyKey = (d: Date) => LocalDate.fromSystemTzDate(d).formatIso()

type ValueOnReport = 'percentage' | 'headcount' | 'raw'

function getOccupancyAverage(
  row: OccupancyReportRow,
  dates: Date[]
): number | null {
  const capacitySum = dates.reduce(
    (sum, date) =>
      sum + (row.occupancies[toOccupancyKey(date)]?.percentage ?? 0),
    0
  )

  const caretakersSum = dates.reduce(
    (sum, date) =>
      sum +
      (typeof row.occupancies[toOccupancyKey(date)]?.percentage === 'number'
        ? 1
        : 0),
    0
  )

  if (caretakersSum > 0) {
    return capacitySum / caretakersSum
  } else {
    return null
  }
}

function getHeadCountAverage(
  row: OccupancyReportRow,
  dates: Date[]
): number | null {
  const headCountSum = dates.reduce(
    (sum, date) =>
      sum + (row.occupancies[toOccupancyKey(date)]?.headcount ?? 0),
    0
  )

  if (dates.length > 0) {
    return headCountSum / dates.length
  } else {
    return null
  }
}

interface DisplayCell {
  value: string
  borderEdge?: 'left' | 'right'
  tooltip?: string
}

function getDisplayCells(
  i18n: Translations,
  reportRows: OccupancyReportRow[],
  dates: Date[],
  usedValues: ValueOnReport
): DisplayCell[][] {
  return reportRows.map((row) => {
    const nameCells: DisplayCell[] =
      'groupId' in row
        ? [
            { value: row.areaName },
            { value: row.unitName },
            { value: row.groupName }
          ]
        : [{ value: row.areaName }, { value: row.unitName }]
    if (usedValues === 'raw') {
      const cells = [...nameCells]
      for (const date of dates) {
        const occupancy = row.occupancies[toOccupancyKey(date)]
        cells.push({
          value:
            typeof occupancy?.sumUnder3y === 'number'
              ? occupancy.sumUnder3y.toFixed(2).replace('.', ',')
              : '0',
          borderEdge: 'left',
          tooltip: i18n.reports.occupancies.sumUnder3y
        })
        cells.push({
          value:
            typeof occupancy?.sumOver3y === 'number'
              ? occupancy.sumOver3y.toFixed(2).replace('.', ',')
              : '0',
          tooltip: i18n.reports.occupancies.sumOver3y
        })
        cells.push({
          value:
            typeof occupancy?.sum === 'number'
              ? occupancy.sum.toFixed(2).replace('.', ',')
              : '0',
          tooltip: i18n.reports.occupancies.sum
        })
        cells.push({
          value:
            typeof occupancy?.caretakers === 'number'
              ? occupancy.caretakers.toFixed(2).replace('.', ',')
              : '0',
          borderEdge: 'right',
          tooltip: i18n.reports.occupancies.caretakers
        })
      }
      return cells
    } else {
      const average =
        usedValues === 'headcount'
          ? getHeadCountAverage(row, dates)
          : getOccupancyAverage(row, dates)

      return [
        ...nameCells,

        // average
        { value: formatAverage(average, usedValues) },

        // daily values
        ...dates.map((date) => {
          const occupancy = row.occupancies[toOccupancyKey(date)]

          if (usedValues === 'percentage') {
            if (!occupancy) {
              return { value: '' }
            }

            if (!occupancy.caretakers) {
              return { value: caretakersMissingSymbol }
            }

            return {
              value: formatPercentage(occupancy.percentage) ?? ''
            }
          } else {
            return {
              value: occupancy?.[usedValues]?.toString() ?? ''
            }
          }
        })
      ]
    }
  })
}

type Averages = {
  average: number | null
  byArea: Record<
    string,
    { average: number | null; byDate: Record<string, number | null> }
  >
}

interface Division {
  dividend: number
  divider: number
}

function calculateAverages(
  reportRows: OccupancyReportRow[],
  dates: Date[],
  usedValues: ValueOnReport
): Averages | null {
  if (usedValues === 'raw') {
    return null
  }
  const dateData: Record<string, Record<string, Division>> = {}
  const areaData = reportRows.reduce<Record<string, Division>>((data, row) => {
    const areaKey = row.areaName
    data[areaKey] = data[areaKey] ?? { dividend: 0, divider: 0 }

    const division = dates.reduce<Division>(
      (prev, date) => {
        const dateKey = toOccupancyKey(date)
        const dividend =
          usedValues === 'headcount'
            ? row.occupancies[dateKey]?.headcount ?? 0
            : row.occupancies[dateKey]?.percentage ?? 0
        const divider =
          usedValues === 'headcount'
            ? 1
            : typeof row.occupancies[dateKey]?.percentage === 'number'
              ? 1
              : 0

        dateData[areaKey] = dateData[areaKey] ?? {}
        dateData[areaKey][dateKey] = dateData[areaKey][dateKey] ?? {
          dividend: 0,
          divider: 0
        }
        dateData[areaKey][dateKey].dividend += dividend
        dateData[areaKey][dateKey].divider += divider

        return {
          dividend: prev.dividend + dividend,
          divider: prev.divider + divider
        }
      },
      { dividend: 0, divider: 0 }
    )
    data[areaKey].dividend += division.dividend
    data[areaKey].divider += division.divider

    return data
  }, {})

  const byAreaAndDate = mapValues(dateData, (dates) =>
    mapValues(dates, (data) =>
      data.divider !== 0 ? data.dividend / data.divider : null
    )
  )
  const byArea = mapValues(areaData, (data, areaKey) => ({
    average: data.divider !== 0 ? data.dividend / data.divider : null,
    byDate: byAreaAndDate[areaKey]
  }))
  const averageData = Object.values(byArea)
    .map((data) => data.average)
    .filter((average): average is number => average !== null)
  const average =
    averageData.length > 0
      ? averageData.reduce((a, b) => a + b, 0) / averageData.length
      : null

  return { average, byArea }
}

function formatAverage(
  average: number | null,
  usedValues: ValueOnReport
): string {
  const roundedAverage =
    average !== null ? Math.round(10 * average) / 10 : undefined
  return (
    (usedValues === 'percentage'
      ? formatPercentage(roundedAverage)
      : formatDecimal(roundedAverage)) ?? ''
  )
}

type ReportMode = {
  display: DisplayMode
  type: OccupancyType
}

export default React.memo(function Occupancies() {
  const { i18n } = useTranslation()
  const areas = useQueryResult(areaQuery())
  const now = mockNow() ?? new Date()
  const [filters, setFilters] = useState<OccupancyReportFilters>({
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    careAreaId: null,
    display: 'UNITS',
    type: 'CONFIRMED',
    providerType: unitProviderTypes.find((type) => type === 'MUNICIPAL'),
    unitTypes: ['CENTRE']
  })
  const [usedValues, setUsedValues] = useState<ValueOnReport>('percentage')
  const [areasOpen, setAreasOpen] = useState<Record<string, boolean>>({})
  const rows = useQueryResult(occupanciesReportQuery({ ...filters }))

  const dates = getDisplayDates(filters.year, filters.month, filters.type)
  const displayCells: DisplayCell[][] = rows
    .map((rs) => getDisplayCells(i18n, rs, dates, usedValues))
    .getOrElse([])
  const displayAreas = displayCells
    .map((cell) => cell[0])
    .filter(
      ({ value }, index, self) =>
        self.findIndex((cell) => cell.value === value) === index
    )
  const averages = rows
    .map((rs) => calculateAverages(rs, dates, usedValues))
    .getOrElse<Averages>({ average: null, byArea: {} })
  const dateCols = dates.reduce((cols, date) => {
    cols.push(date)
    if (usedValues === 'raw') {
      cols.push(date, date, date)
    }
    return cols
  }, [] as Date[])

  const includeGroups = filters.type.startsWith('GROUP_')

  const careAreaAll = { id: undefined, name: i18n.common.all }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.occupancies.title}</Title>
        {renderResult(areas, (areas) => (
          <>
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
              <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
              <Wrapper>
                <Combobox
                  items={[careAreaAll, ...areas]}
                  onChange={(item) => {
                    if (item) {
                      setFilters({ ...filters, careAreaId: item.id })
                    }
                  }}
                  selectedItem={
                    filters.careAreaId === undefined
                      ? careAreaAll
                      : areas.find((area) => area.id === filters.careAreaId) ??
                        null
                  }
                  placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
                  getItemLabel={(item) => item.name}
                />
              </Wrapper>
            </FilterRow>
            <FilterRow>
              <FilterLabel>{i18n.reports.common.unitProviderType}</FilterLabel>
              <Wrapper>
                <Combobox
                  items={[
                    {
                      value: undefined,
                      label: i18n.common.all
                    },
                    ...unitProviderTypes.map((providerType) => ({
                      value: providerType,
                      label: i18n.reports.common.unitProviderTypes[providerType]
                    }))
                  ]}
                  selectedItem={{
                    value: filters.providerType,
                    label: filters.providerType
                      ? i18n.reports.common.unitProviderTypes[
                          filters.providerType
                        ]
                      : i18n.common.all
                  }}
                  onChange={(value) => {
                    if (value) {
                      setFilters({
                        ...filters,
                        providerType: value.value
                      })
                    }
                  }}
                  getItemLabel={(item) => item.label}
                />
              </Wrapper>
            </FilterRow>
            <FilterRow>
              <FilterLabel>{i18n.reports.common.unitType}</FilterLabel>
              <Wrapper>
                <MultiSelect
                  options={careTypes}
                  onChange={(selectedItems) =>
                    setFilters({
                      ...filters,
                      unitTypes: selectedItems.map(
                        (selectedItem) => selectedItem
                      )
                    })
                  }
                  value={careTypes.filter(
                    (unitType) => filters.unitTypes?.includes(unitType) ?? true
                  )}
                  getOptionId={(unitType) => unitType}
                  getOptionLabel={(unitType) => i18n.common.types[unitType]}
                  placeholder=""
                />
              </Wrapper>
            </FilterRow>
            <FilterRow>
              <FilterLabel>{i18n.reports.occupancies.filters.type}</FilterLabel>
              <Wrapper>
                <Combobox<ReportMode>
                  items={[
                    { display: 'UNITS', type: 'CONFIRMED' },
                    { display: 'UNITS', type: 'PLANNED' },
                    { display: 'UNITS', type: 'REALIZED' },
                    { display: 'GROUPS', type: 'CONFIRMED' },
                    { display: 'GROUPS', type: 'REALIZED' }
                  ]}
                  selectedItem={{
                    display: filters.display,
                    type: filters.type
                  }}
                  onChange={(value) => {
                    if (value) {
                      setFilters({
                        ...filters,
                        display: value.display,
                        type: value.type
                      })
                    }
                  }}
                  getItemLabel={({ display, type }) =>
                    i18n.reports.occupancies.filters.types[display][type]
                  }
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
                        i18n.reports.occupancies.filters.valuesOnReport
                          .percentage
                    },
                    {
                      value: 'headcount' as ValueOnReport,
                      label:
                        i18n.reports.occupancies.filters.valuesOnReport
                          .headcount
                    },
                    {
                      value: 'raw' as ValueOnReport,
                      label: i18n.reports.occupancies.filters.valuesOnReport.raw
                    }
                  ]}
                  selectedItem={{
                    value: usedValues,
                    label:
                      i18n.reports.occupancies.filters.valuesOnReport[
                        usedValues
                      ]
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
          </>
        ))}

        {renderResult(combine(rows, areas), ([rows, areas]) => (
          <>
            {filters.careAreaId !== null && (
              <>
                <ReportDownload
                  data={[
                    [
                      i18n.reports.common.careAreaName,
                      i18n.reports.common.unitName,
                      includeGroups ? i18n.reports.common.groupName : undefined,
                      usedValues !== 'raw'
                        ? i18n.reports.occupancies.average
                        : undefined,
                      ...dateCols.map((date) => formatDate(date, 'dd.MM.'))
                    ].filter((label) => label !== undefined),
                    ...displayCells.map((row) =>
                      row.map((column) => column.value)
                    )
                  ]}
                  filename={getFilename(
                    i18n,
                    filters.year,
                    filters.month,
                    filters.display,
                    filters.type,
                    filters.careAreaId === undefined
                      ? i18n.common.all
                      : areas.find((area) => area.id == filters.careAreaId)
                          ?.name ?? ''
                  )}
                />
                <TableScrollable>
                  <Thead>
                    <Tr>
                      <Th>
                        {filters.careAreaId === undefined
                          ? i18n.reports.occupancies.unitsGroupedByArea
                          : i18n.reports.common.unitName}
                      </Th>
                      {includeGroups && (
                        <Th>{i18n.reports.common.groupName}</Th>
                      )}
                      {usedValues !== 'raw' && (
                        <Th>{i18n.reports.occupancies.average}</Th>
                      )}
                      {dates.map((date) => (
                        <Th
                          align="center"
                          key={date.toDateString()}
                          colSpan={usedValues === 'raw' ? 4 : undefined}
                        >
                          {formatDate(date, 'dd.MM.')}
                        </Th>
                      ))}
                    </Tr>
                  </Thead>
                  <Tbody>
                    {displayAreas.map(({ value: areaName }) => (
                      <React.Fragment key={areaName}>
                        {filters.careAreaId === undefined && (
                          <Tr>
                            <StyledTd
                              colSpan={
                                usedValues === 'raw'
                                  ? 4 + dateCols.length
                                  : undefined
                              }
                            >
                              <div
                                onClick={() =>
                                  setAreasOpen({
                                    ...areasOpen,
                                    [areaName]: !(areasOpen[areaName] ?? false)
                                  })
                                }
                              >
                                <span>
                                  <AccordionIcon
                                    icon={
                                      areasOpen[areaName]
                                        ? faChevronUp
                                        : faChevronDown
                                    }
                                  />
                                </span>
                                <span>{areaName}</span>
                              </div>
                            </StyledTd>
                            {usedValues !== 'raw' && (
                              <>
                                <StyledTd>
                                  {formatAverage(
                                    averages?.byArea[areaName]?.average ?? null,
                                    usedValues
                                  )}
                                </StyledTd>
                                {dateCols.map((dateCol) => (
                                  <StyledTd key={dateCol.toDateString()}>
                                    {formatAverage(
                                      averages?.byArea[areaName]?.byDate[
                                        toOccupancyKey(dateCol)
                                      ] ?? null,
                                      usedValues
                                    )}
                                  </StyledTd>
                                ))}
                              </>
                            )}
                          </Tr>
                        )}
                        {rows.map(
                          (row, rowNum) =>
                            row.areaName === areaName &&
                            (filters.careAreaId !== undefined ||
                              areasOpen[areaName]) && (
                              <Tr
                                key={isGroupRow(row) ? row.groupId : row.unitId}
                              >
                                <StyledTd>
                                  <Link to={`/units/${row.unitId}`}>
                                    {row.unitName}
                                  </Link>
                                </StyledTd>
                                {displayCells[rowNum]
                                  .slice(2)
                                  .map((cell, colNum) => (
                                    <StyledTd
                                      key={colNum}
                                      borderEdge={cell.borderEdge}
                                    >
                                      {cell.tooltip ? (
                                        <Tooltip tooltip={cell.tooltip}>
                                          {cell.value}
                                        </Tooltip>
                                      ) : (
                                        <>{cell.value}</>
                                      )}
                                    </StyledTd>
                                  ))}
                              </Tr>
                            )
                        )}
                      </React.Fragment>
                    ))}
                  </Tbody>
                  {usedValues !== 'raw' && (
                    <StyledTfoot>
                      <Tr>
                        <Td colSpan={includeGroups ? 4 : undefined}>
                          {i18n.reports.common.total}
                        </Td>
                        <Td colSpan={1 + dateCols.length}>
                          {formatAverage(averages?.average ?? null, usedValues)}
                        </Td>
                      </Tr>
                    </StyledTfoot>
                  )}
                </TableScrollable>
                <Gap size="s" />
                <Legend>
                  <i>
                    {`${caretakersMissingSymbol} = ${i18n.reports.occupancies.missingCaretakersLegend}`}
                  </i>
                </Legend>
              </>
            )}
          </>
        ))}
      </ContentArea>
    </Container>
  )
})

const caretakersMissingSymbol = 'X'

const Legend = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  color: ${(p) => p.theme.colors.grayscale.g70};
`
