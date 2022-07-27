// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { addDays, isAfter, isWeekend, lastDayOfMonth } from 'date-fns'
import mapValues from 'lodash/mapValues'
import range from 'lodash/range'
import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled, { css } from 'styled-components'

import type { Result } from 'lib-common/api'
import { Loading, Success } from 'lib-common/api'
import { formatDate } from 'lib-common/date'
import type { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { formatPercentage, formatDecimal } from 'lib-common/utils/number'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Tfoot, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import { unitProviderTypes } from 'lib-customizations/employee'
import { faChevronDown, faChevronUp } from 'lib-icons'

import { getAreas } from '../../api/daycare'
import type {
  OccupancyReportFilters,
  OccupancyReportRow,
  OccupancyReportType
} from '../../api/reports'
import { getOccupanciesReport } from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, TableScrollable } from './common'

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
  const time = LocalDate.of(year, month, 1).formatExotic('yyyy-MM')
  return `${prefix}-${time}-${careAreaName}.csv`.replace(/ /g, '_')
}

const toOccupancyKey = (d: Date) => LocalDate.fromSystemTzDate(d).formatIso()

type ValueOnReport = 'percentage' | 'headcount' | 'raw'

function getOccupancyAverage(
  row: OccupancyReportRow,
  dates: Date[]
): number | null {
  const capacitySum = dates.reduce((sum, date) => {
    return sum + (row.occupancies[toOccupancyKey(date)]?.sum ?? 0)
  }, 0)

  const caretakersSum = dates.reduce((sum, date) => {
    return sum + (row.occupancies[toOccupancyKey(date)]?.caretakers ?? 0)
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
    return sum + (row.occupancies[toOccupancyKey(date)]?.headcount ?? 0)
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
    const nameCells =
      'groupName' in row
        ? [row.areaName, row.unitName, row.groupName]
        : [row.areaName, row.unitName]
    if (usedValues === 'raw') {
      const cells = [...nameCells]
      for (const date of dates) {
        const occupancy = row.occupancies[toOccupancyKey(date)]
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

      return [
        ...nameCells,

        // average
        formatAverage(average, usedValues),

        // daily values
        ...dates.map((date) => {
          const occupancy = row.occupancies[toOccupancyKey(date)]

          if (usedValues === 'percentage') {
            if (!occupancy) {
              return ''
            }

            if (!occupancy.caretakers) {
              return caretakersMissingSymbol
            }

            return formatPercentage(occupancy.percentage ?? undefined) ?? ''
          } else {
            return occupancy?.[usedValues]?.toString() ?? ''
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
            : 100 * (row.occupancies[dateKey]?.sum ?? 0)
        const divider =
          usedValues === 'headcount'
            ? 1
            : resolveCaretakers(row.occupancies[dateKey]?.caretakers)

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

function resolveCaretakers(caretakers: number | null | undefined): number {
  if (caretakers === null || caretakers === undefined) {
    return 0
  }
  return 7 * caretakers
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

export default React.memo(function Occupancies() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<OccupancyReportRow[]>>(Success.of([]))
  const [areas, setAreas] = useState<DaycareCareArea[]>([])
  const [filters, setFilters] = useState<OccupancyReportFilters>({
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    careAreaId: null,
    type: 'UNIT_CONFIRMED'
  })
  const [usedValues, setUsedValues] = useState<ValueOnReport>('percentage')
  const [areasOpen, setAreasOpen] = useState<Record<string, boolean>>({})

  useEffect(() => {
    void getAreas().then((res) => res.isSuccess && setAreas(res.value))
  }, [])

  useEffect(() => {
    if (filters.careAreaId === null) return

    setRows(Loading.of())
    void getOccupanciesReport(filters).then(setRows)
  }, [filters])

  const dates = getDisplayDates(filters.year, filters.month, filters.type)
  const displayCells: string[][] = rows
    .map((rs) => getDisplayCells(rs, dates, usedValues))
    .getOrElse([])
  const displayAreas = displayCells
    .map((cell) => cell[0])
    .filter((value, index, self) => self.indexOf(value) === index)
  const averages = rows
    .map((rs) => calculateAverages(rs, dates, usedValues))
    .getOrElse<Averages>({ average: null, byArea: {} })
  const dateCols = dates.reduce((cols, date) => {
    cols.push(date)
    if (usedValues === 'raw') cols.push(date)
    return cols
  }, [] as Date[])

  const includeGroups = filters.type.startsWith('GROUP_')

  const careAreaAll = { id: undefined, name: i18n.common.all }

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
                  : areas.find((area) => area.id === filters.careAreaId) ?? null
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
                  ? i18n.reports.common.unitProviderTypes[filters.providerType]
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
        {rows.isSuccess && filters.careAreaId !== null && (
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
                ...displayCells
              ]}
              filename={getFilename(
                i18n,
                filters.year,
                filters.month,
                filters.type,
                filters.careAreaId === undefined
                  ? i18n.common.all
                  : areas.find((area) => area.id == filters.careAreaId)?.name ??
                      ''
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
                  {includeGroups && <Th>{i18n.reports.common.groupName}</Th>}
                  {usedValues !== 'raw' && (
                    <Th>{i18n.reports.occupancies.average}</Th>
                  )}
                  {dates.map((date) => (
                    <Th
                      align="center"
                      key={date.toDateString()}
                      colSpan={usedValues === 'raw' ? 2 : undefined}
                    >
                      {formatDate(date, 'dd.MM.')}
                    </Th>
                  ))}
                </Tr>
              </Thead>
              <Tbody>
                {displayAreas.map((areaName) => (
                  <React.Fragment key={areaName}>
                    {filters.careAreaId === undefined && (
                      <Tr>
                        <StyledTd
                          colSpan={
                            usedValues === 'raw'
                              ? 2 + dateCols.length
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
                              ></AccordionIcon>
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
                    {rows.value.map(
                      (row, rowNum) =>
                        row.areaName === areaName &&
                        (filters.careAreaId !== undefined ||
                          areasOpen[areaName]) && (
                          <Tr key={row.unitId}>
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
                                  borderEdge={
                                    usedValues === 'raw'
                                      ? colNum % 2 === (includeGroups ? 1 : 0)
                                        ? 'left'
                                        : 'right'
                                      : undefined
                                  }
                                >
                                  {usedValues === 'raw' &&
                                  (!includeGroups || colNum > 0) ? (
                                    <Tooltip
                                      tooltip={
                                        colNum % 2 === (includeGroups ? 1 : 0)
                                          ? i18n.reports.occupancies.sum
                                          : i18n.reports.occupancies.caretakers
                                      }
                                    >
                                      {cell}
                                    </Tooltip>
                                  ) : (
                                    <>{cell}</>
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
                    <Td colSpan={includeGroups ? 2 : undefined}>
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
