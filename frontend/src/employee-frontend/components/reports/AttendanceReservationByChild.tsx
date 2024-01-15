// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { DefaultTheme, useTheme } from 'styled-components'

import { Loading } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AttendanceReservationReportByChildRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { StaticChip } from 'lib-components/atoms/Chip'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { Translations } from 'lib-customizations/employee'

import {
  AttendanceReservationReportFilters,
  getAssistanceReservationReportByChild
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'
import { unitGroupsQuery, unitsQuery } from '../unit/queries'

import { AttendanceReservationReportTd } from './AttendanceReservation'
import { FilterLabel, FilterRow, TableScrollable } from './common'

const dateFormat = 'EEEEEE d.M.'
const timeFormat = 'HH:mm'

type OrderBy = 'start' | 'end'
const orderByOptions: OrderBy[] = ['start', 'end']

export default React.memo(function AttendanceReservationByChild() {
  const { lang, i18n } = useTranslation()
  const theme = useTheme()

  const [unitId, setUnitId] = useState<UUID | null>(null)
  const [filters, setFilters] = useState<AttendanceReservationReportFilters>(
    () => {
      const defaultDate = LocalDate.todayInSystemTz().addWeeks(1)

      return {
        range: new FiniteDateRange(
          defaultDate.startOfWeek(),
          defaultDate.endOfWeek()
        ),
        groupIds: []
      }
    }
  )
  const [orderBy, setOrderBy] = useState<OrderBy>('start')

  const units = useQueryResult(unitsQuery())
  const groups = useQueryResult(queryOrDefault(unitGroupsQuery, [])(unitId))
  const [report] = useApiState(
    () =>
      unitId !== null
        ? getAssistanceReservationReportByChild(unitId, filters)
        : Promise.resolve(
            Loading.of<AttendanceReservationReportByChildRow[]>()
          ),
    [unitId, filters]
  )

  const sortedRows = report
    .map<
      AttendanceReservationReportByChildRow[]
    >((rows) => rows.sort((a, b) => compareByGroup(a, b, lang) || compareByDate(a, b) || (orderBy === 'start' ? compareByStartTime(a, b) : 0) || (orderBy === 'end' ? compareByEndTime(a, b) : 0) || compareByChildName(a, b, lang)))
    .getOrElse<AttendanceReservationReportByChildRow[]>([])
  const sortedUnits = units
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])
  const sortedGroups = groups
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])

  const dates = sortedRows
    .map((row) => row.date)
    .filter(
      (value, index, array) =>
        array.findIndex((date) => date.isEqual(value)) === index
    )
    .sort((a, b) => a.compareTo(b))

  const rowsByGroup = sortedRows.reduce<
    Record<string, (AttendanceReservationReportByChildRow | undefined)[][]>
  >((data, row) => {
    const groupKey = row.groupId ?? 'ungrouped'
    const rows = data[groupKey] ?? []
    const columnIndex = dates.findIndex((date) => date.isEqual(row.date))
    const rowIndex = rows.findIndex((row) => row[columnIndex] === undefined)
    if (rowIndex !== -1) {
      rows[rowIndex][columnIndex] = row
    } else {
      const columns = []
      dates.forEach(() => columns.push(undefined))
      columns[columnIndex] = row
      rows.push(columns)
    }
    data[groupKey] = rows
    return data
  }, {})
  const entries = Object.entries(rowsByGroup)
  const showGroupTitle = entries.length > 1

  const tableComponents = entries.map(([groupId, rows]) => {
    const groupName = showGroupTitle
      ? sortedGroups.find((group) => group.id === groupId)?.name ??
        i18n.reports.attendanceReservationByChild.ungrouped
      : undefined
    return (
      <TableScrollable
        key={groupId}
        data-qa="report-attendance-reservation-by-child-table"
      >
        <caption>{groupName}</caption>
        <Thead sticky>
          <Tr>
            {dates.map((date) => (
              <Th key={date.formatIso()} colSpan={3} align="center">
                {date.format(dateFormat, lang)}
              </Th>
            ))}
          </Tr>
          <Tr>
            {dates.map((date) => (
              <React.Fragment key={date.formatIso()}>
                <Th>{i18n.reports.common.child}</Th>
                <Th>
                  {
                    i18n.reports.attendanceReservationByChild
                      .reservationStartTime
                  }
                </Th>
                <Th>
                  {i18n.reports.attendanceReservationByChild.reservationEndTime}
                </Th>
              </React.Fragment>
            ))}
          </Tr>
        </Thead>
        <Tbody>{getTableBody(rows, dates, i18n, theme)}</Tbody>
      </TableScrollable>
    )
  })

  const periodAriaId = useUniqueId()

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>
          {i18n.reports.attendanceReservationByChild.title}
        </Title>

        <FilterRow>
          <FilterLabel id={periodAriaId}>
            {i18n.reports.common.period}
          </FilterLabel>
          <FlexRow>
            <DateRangePicker
              start={filters.range.start}
              end={filters.range.end}
              onChange={(start, end) => {
                if (start !== null && end !== null) {
                  setFilters({
                    ...filters,
                    range: new FiniteDateRange(start, end)
                  })
                }
              }}
              locale={lang}
              required={true}
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <FlexRow>
            <Combobox
              items={sortedUnits}
              onChange={(selectedItem) => {
                setUnitId(selectedItem !== null ? selectedItem.id : null)
                setFilters({ ...filters, groupIds: [] })
              }}
              selectedItem={
                sortedUnits.find((unit) => unit.id === unitId) ?? null
              }
              getItemLabel={(item) => item.name}
              placeholder={i18n.filters.unitPlaceholder}
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.groupName}</FilterLabel>
          <div style={{ width: '100%' }}>
            <MultiSelect
              options={sortedGroups}
              onChange={(selectedItems) =>
                setFilters({
                  ...filters,
                  groupIds: selectedItems.map((selectedItem) => selectedItem.id)
                })
              }
              value={sortedGroups.filter((group) =>
                filters.groupIds.includes(group.id)
              )}
              getOptionId={(group) => group.id}
              getOptionLabel={(group) => group.name}
              placeholder=""
              isClearable={true}
            />
          </div>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.orderBy}</FilterLabel>
          <FlexRow>
            <Combobox
              items={orderByOptions}
              onChange={(selectedItem) => {
                if (selectedItem !== null) {
                  setOrderBy(selectedItem)
                }
              }}
              selectedItem={
                orderByOptions.find((option) => option === orderBy) ?? null
              }
              getItemLabel={(item) =>
                i18n.reports.attendanceReservationByChild.orderByOptions[item]
              }
            />
          </FlexRow>
        </FilterRow>

        {unitId !== null && report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <ReportDownload
              data={sortedRows.map((row) => ({
                ...row,
                childName: getChildName(row),
                date: row.date.format(),
                absenceType:
                  row.absenceType !== null
                    ? i18n.absences.absenceTypes[row.absenceType]
                    : '',
                reservationStartTime:
                  row.absenceType === null
                    ? row.reservationStartTime?.format(timeFormat)
                    : '',
                reservationEndTime:
                  row.absenceType === null
                    ? row.reservationEndTime?.format(timeFormat)
                    : ''
              }))}
              headers={[
                ...(filters.groupIds.length > 0
                  ? [
                      {
                        label: i18n.reports.common.groupName,
                        key: 'groupName' as const
                      }
                    ]
                  : []),
                {
                  label: i18n.reports.common.child,
                  key: 'childName'
                },
                {
                  label: i18n.reports.common.date,
                  key: 'date'
                },
                {
                  label: i18n.reports.attendanceReservationByChild.absenceType,
                  key: 'absenceType'
                },
                {
                  label:
                    i18n.reports.attendanceReservationByChild
                      .reservationStartTime,
                  key: 'reservationStartTime'
                },
                {
                  label:
                    i18n.reports.attendanceReservationByChild
                      .reservationEndTime,
                  key: 'reservationEndTime'
                }
              ]}
              filename={`${i18n.reports.attendanceReservationByChild.title} ${
                sortedUnits.find((unit) => unit.id === unitId)?.name ?? ''
              } ${filters.range.start.formatIso()}-${filters.range.end.formatIso()}.csv`}
            />
            {tableComponents}
          </>
        )}
      </ContentArea>
    </Container>
  )
})

const getTableBody = (
  rows: (AttendanceReservationReportByChildRow | undefined)[][],
  dates: LocalDate[],
  i18n: Translations,
  theme: DefaultTheme
) => {
  const components: React.ReactNode[] = []
  rows.forEach((row, rowIndex) => {
    components.push(
      <Tr key={`row-${rowIndex}`}>
        {getTableRow(row, rowIndex, dates, i18n, theme)}
      </Tr>
    )
  })
  return components
}

const getTableRow = (
  row: (AttendanceReservationReportByChildRow | undefined)[],
  rowIndex: number,
  dates: LocalDate[],
  i18n: Translations,
  theme: DefaultTheme
) => {
  const components: React.ReactNode[] = []
  const isFirstRow = rowIndex === 0
  row.forEach((column, columnIndex) => {
    const date = dates[columnIndex]
    const isToday = date.isToday()
    const isFuture = date.isAfter(LocalDate.todayInHelsinkiTz())
    components.push(
      <React.Fragment key={`row-${rowIndex}-column-${columnIndex}`}>
        <AttendanceReservationReportTd
          borderEdge={[
            'left',
            ...(isToday && isFirstRow ? (['top'] as const) : [])
          ]}
          isToday={isToday}
          isFuture={isFuture}
        >
          {column && (
            <>
              {column.isBackupCare && (
                <StaticChip color={theme.colors.main.m1}>v</StaticChip>
              )}
              <Link to={`/child-information/${column.childId}`}>
                {getChildName(column)}
              </Link>
            </>
          )}
        </AttendanceReservationReportTd>
        {column !== undefined && (
          <React.Fragment>
            {column.absenceType !== null && (
              <AttendanceReservationReportTd
                borderEdge={[
                  'right',
                  ...(isToday && isFirstRow ? (['top'] as const) : [])
                ]}
                isToday={isToday}
                isFuture={isFuture}
                colSpan={2}
              >
                {i18n.absences.absenceTypes[column.absenceType]}
              </AttendanceReservationReportTd>
            )}
            {column.absenceType === null && (
              <React.Fragment>
                <AttendanceReservationReportTd
                  borderEdge={isToday && isFirstRow ? ['top'] : []}
                  isToday={isToday}
                  isFuture={isFuture}
                >
                  {column.reservationStartTime?.format(timeFormat)}
                </AttendanceReservationReportTd>
                <AttendanceReservationReportTd
                  borderEdge={[
                    'right',
                    ...(isToday && isFirstRow ? (['top'] as const) : [])
                  ]}
                  isToday={isToday}
                  isFuture={isFuture}
                >
                  {column.reservationEndTime?.format(timeFormat)}
                </AttendanceReservationReportTd>
              </React.Fragment>
            )}
          </React.Fragment>
        )}
        {column === undefined && (
          <AttendanceReservationReportTd
            borderEdge={[
              'right',
              ...(isToday && isFirstRow ? (['top'] as const) : [])
            ]}
            isToday={isToday}
            isFuture={isFuture}
            colSpan={2}
          />
        )}
      </React.Fragment>
    )
  })
  return components
}

const compareByGroup = (
  a: AttendanceReservationReportByChildRow,
  b: AttendanceReservationReportByChildRow,
  lang: string
) => (a.groupName ?? '').localeCompare(b.groupName ?? '', lang)

const compareByDate = (
  a: AttendanceReservationReportByChildRow,
  b: AttendanceReservationReportByChildRow
) => a.date.compareTo(b.date)

const compareByStartTime = (
  a: AttendanceReservationReportByChildRow,
  b: AttendanceReservationReportByChildRow
) => compareByTime(a.reservationStartTime, b.reservationStartTime)

const compareByEndTime = (
  a: AttendanceReservationReportByChildRow,
  b: AttendanceReservationReportByChildRow
) => compareByTime(a.reservationEndTime, b.reservationEndTime)

const compareByTime = (a: LocalTime | null, b: LocalTime | null) => {
  if (a !== null && b !== null) {
    return a.compareTo(b)
  }
  if (a !== null) {
    return -1
  }
  if (b !== null) {
    return 1
  }
  return 0
}

const compareByChildName = (
  a: AttendanceReservationReportByChildRow,
  b: AttendanceReservationReportByChildRow,
  lang: string
) => getChildName(a).localeCompare(getChildName(b), lang)

const getChildName = (row: AttendanceReservationReportByChildRow) =>
  `${row.childLastName} ${row.childFirstName}`.trim()
