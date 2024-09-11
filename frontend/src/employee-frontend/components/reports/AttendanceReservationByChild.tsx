// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { groupBy } from 'lodash/fp'
import sortBy from 'lodash/sortBy'
import unzip from 'lodash/unzip'
import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTheme } from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AttendanceReservationReportByChildBody,
  AttendanceReservationReportByChildGroup,
  AttendanceReservationReportByChildItem
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { constantQuery, useQueryResult } from 'lib-common/query'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { StaticChip } from 'lib-components/atoms/Chip'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'

import { Translations, useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { unitGroupsQuery, unitsQuery } from '../unit/queries'

import { AttendanceReservationReportTd } from './AttendanceReservation'
import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { attendanceReservationReportByChildQuery } from './queries'

// lodash's `unzip` is equivalent to a transpose operation on an array of
// arrays. It forces the arrays to the same length and adds undefineds for
// missing items. This is not reflected by unzip's type, so a type annotation
// is needed.
const transpose: <T>(arr: T[][]) => (T | undefined)[][] = unzip

const dateFormat = 'EEEEEE d.M.'

type OrderBy = 'start' | 'end'
const orderByOptions: OrderBy[] = ['start', 'end']

export default React.memo(function AttendanceReservationByChild() {
  const { lang, i18n } = useTranslation()

  const [unitId, setUnitId] = useState<UUID | null>(null)
  const [range, setRange] = useState(() => {
    const defaultDate = LocalDate.todayInSystemTz().addWeeks(1)
    return new FiniteDateRange(
      defaultDate.startOfWeek(),
      defaultDate.endOfWeek()
    )
  })
  const [groupIds, setGroupIds] = useState<UUID[]>([])
  const [orderBy, setOrderBy] = useState<OrderBy>('start')

  const [filterByTime, setFilterByTime] = useState(false)
  const [startTime, setStartTime] = useState<string>('00:00')
  const [endTime, setEndTime] = useState<string>('23:59')

  const units = useQueryResult(unitsQuery({ includeClosed: true }))
  const groups = useQueryResult(
    unitId ? unitGroupsQuery({ daycareId: unitId }) : constantQuery([])
  )

  const [activeParams, setActiveParams] = useState<{
    body: AttendanceReservationReportByChildBody
    orderBy: OrderBy
    timeFilter: TimeRange | undefined
  } | null>(null)

  const search = () => {
    const timeFilterValid =
      !filterByTime ||
      validateTimeFilter(startTime, endTime, i18n) === undefined
    const timeFilter =
      filterByTime && timeFilterValid
        ? new TimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime))
        : undefined

    if (unitId && timeFilterValid) {
      setActiveParams({
        body: { unitId, range, groupIds },
        orderBy,
        timeFilter
      })
    }
  }
  const reportData = useQueryResult(
    activeParams
      ? attendanceReservationReportByChildQuery({ body: activeParams.body })
      : constantQuery([])
  )
  const report = useMemo(
    () =>
      reportData.map((groups) =>
        groups.map((group) =>
          toTableData(group, orderBy, activeParams?.timeFilter)
        )
      ),
    [activeParams?.timeFilter, orderBy, reportData]
  )

  const sortedUnits = units
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])
  const sortedGroups = groups
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])

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
              start={range.start}
              end={range.end}
              onChange={(start, end) => {
                if (start !== null && end !== null) {
                  setRange(new FiniteDateRange(start, end))
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
                setGroupIds([])
              }}
              selectedItem={
                sortedUnits.find((unit) => unit.id === unitId) ?? null
              }
              getItemLabel={(item) => item.name}
              placeholder={i18n.filters.unitPlaceholder}
              data-qa="unit-select"
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.groupName}</FilterLabel>
          <div style={{ width: '100%' }}>
            <MultiSelect
              options={sortedGroups}
              onChange={(selectedItems) =>
                setGroupIds(
                  selectedItems.map((selectedItem) => selectedItem.id)
                )
              }
              value={sortedGroups.filter((group) =>
                groupIds.includes(group.id)
              )}
              getOptionId={(group) => group.id}
              getOptionLabel={(group) => group.name}
              placeholder=""
              isClearable={true}
              data-qa="group-select"
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
        <FilterRow>
          <FilterLabel id={periodAriaId}>
            {i18n.reports.common.clock}
          </FilterLabel>
          <FlexRow>
            <Checkbox
              label={i18n.reports.attendanceReservationByChild.filterByTime}
              checked={filterByTime}
              onChange={setFilterByTime}
              data-qa="filter-by-time"
            />
            {filterByTime && (
              <>
                <TimeInput
                  value={startTime}
                  onChange={(time) => setStartTime(time)}
                  hideErrorsBeforeTouched={true}
                  data-qa="start-time-filter"
                  info={validateTimeFilter(startTime, endTime, i18n)}
                />
                <TimeInput
                  value={endTime}
                  onChange={(time) => setEndTime(time)}
                  hideErrorsBeforeTouched={true}
                  data-qa="end-time-filter"
                  info={validateTimeFilter(startTime, endTime, i18n)}
                />
              </>
            )}
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <Button
            primary
            text={i18n.common.search}
            onClick={search}
            data-qa="search-button"
          />
        </FilterRow>

        {renderResult(report, (report) => {
          if (activeParams === null || report === null) return null
          return (
            <>
              <ReportDownload
                data={report.flatMap(({ groupName, rows }) =>
                  transpose(rows).flatMap((row) =>
                    row.flatMap((item) =>
                      item
                        ? {
                            ...(groupName !== null ? { groupName } : {}),
                            childName: `${item.childLastName} ${item.childFirstName}`,
                            date: item.date.format(),
                            fullDayAbsence: item.fullDayAbsence ? 'Poissa' : '',
                            reservationStartTime: item.fullDayAbsence
                              ? ''
                              : item.reservation?.start.format() ?? '-',
                            reservationEndTime: item.fullDayAbsence
                              ? ''
                              : item.reservation?.end.format() ?? '-'
                          }
                        : []
                    )
                  )
                )}
                headers={[
                  ...(report.length > 0 && report[0].groupId !== null
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
                    label: i18n.reports.attendanceReservationByChild.absence,
                    key: 'fullDayAbsence'
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
                } ${activeParams.body.range.start.formatIso()}-${activeParams.body.range.end.formatIso()}.csv`}
              />
              {report.map(({ groupId, groupName, headings, rows }) => (
                <GroupData
                  key={groupId}
                  groupName={groupName}
                  headings={headings}
                  rows={rows}
                />
              ))}
            </>
          )
        })}
      </ContentArea>
    </Container>
  )
})

function validateTimeFilter(
  startTime: string,
  endTime: string,
  i18n: Translations
): { status: 'warning'; text: string } | undefined {
  const startTimeLocalTime = LocalTime.tryParse(startTime)
  const endTimeLocalTime = LocalTime.tryParse(endTime)
  return !startTimeLocalTime ||
    !endTimeLocalTime ||
    endTimeLocalTime.isEqualOrBefore(startTimeLocalTime)
    ? {
        status: 'warning',
        text: i18n.reports.attendanceReservationByChild.timeFilterError
      }
    : undefined
}

function toTableData(
  group: AttendanceReservationReportByChildGroup,
  orderBy: OrderBy,
  timeFilter: TimeRange | undefined
): {
  groupId: string | null
  groupName: string | null
  headings: LocalDate[]
  rows: (AttendanceReservationReportByChildItem | undefined)[][]
} {
  const filtered =
    timeFilter !== undefined
      ? group.items.filter(
          (item) =>
            item.reservation !== null &&
            timeFilter.intersection(item.reservation) !== undefined
        )
      : group.items
  const sorted = sortBy(filtered, [
    (item) => item.date.formatIso(),
    (item) => {
      if (item.fullDayAbsence) return 'z' // absences last
      if (!item.reservation) return 'y' // missing reservation second last
      if (orderBy === 'start') return item.reservation.start.formatIso()
      return item.reservation.end.formatIso()
    }
  ])

  // We want a table like this:
  //
  // | date1 | date2 | date3 |
  // +-------+-------+-------+
  // | item1 | item2 | item3 |
  // | item1 |       | item3 |

  // Currently we have:
  //
  // sorted = [item1, item1, item2, item3, item3]

  const grouped = groupBy((item) => item.date.formatIso(), sorted)
  // grouped = {
  //   date1: [item1, item1],
  //   date2: [item2],
  //   date3: [item3, item3]
  // }

  const values = Object.values(grouped)
  // values = [
  //   [item1, item1]
  //   [item2]
  //   [item3, item3]
  // ]

  const rows = transpose(values)
  // rows = [
  //   [item1, item2, item3]
  //   [item1, undef, item3]
  // ]

  const headings = Object.keys(grouped).map((date) => LocalDate.parseIso(date))
  // headings = [date1, date2, date3]

  return {
    groupId: group.groupId,
    groupName: group.groupName,
    headings,
    rows
  }
}

const GroupData = React.memo(function GroupData({
  groupName,
  headings,
  rows
}: {
  groupName: string | null
  headings: LocalDate[]
  rows: (AttendanceReservationReportByChildItem | undefined)[][]
}) {
  const { lang, i18n } = useTranslation()
  return (
    <TableScrollable>
      {groupName !== null && <caption>{groupName}</caption>}
      <Thead sticky>
        <Tr>
          {headings.map((date) => (
            <Th key={date.formatIso()} colSpan={3} align="center">
              {date.format(dateFormat, lang)}
            </Th>
          ))}
        </Tr>
        <Tr>
          {headings.map((date) => (
            <React.Fragment key={date.formatIso()}>
              <Th>{i18n.reports.common.child}</Th>
              <Th>
                {i18n.reports.attendanceReservationByChild.reservationStartTime}
              </Th>
              <Th>
                {i18n.reports.attendanceReservationByChild.reservationEndTime}
              </Th>
            </React.Fragment>
          ))}
        </Tr>
      </Thead>
      <Tbody>
        {rows.map((row, index) => (
          <Row key={index} items={row} isFirstRow={index === 0} />
        ))}
      </Tbody>
    </TableScrollable>
  )
})

const Row = React.memo(function Row({
  items,
  isFirstRow
}: {
  items: (AttendanceReservationReportByChildItem | undefined)[]
  isFirstRow: boolean
}) {
  const { i18n } = useTranslation()
  const theme = useTheme()
  const today = LocalDate.todayInHelsinkiTz()

  return (
    <Tr data-qa="child-attendance-reservation-row">
      {items.map((item, index) => {
        if (!item) {
          return (
            <AttendanceReservationReportTd
              key={index}
              borderEdge={['left' as const, 'right' as const]}
              isToday={false}
              isFuture={false}
              colSpan={3}
            />
          )
        }

        const isToday = item.date.isEqual(today)
        const isFuture = item.date.isAfter(today)

        return (
          <React.Fragment key={item.date.formatIso()}>
            <AttendanceReservationReportTd
              borderEdge={[
                'left' as const,
                ...(isToday && isFirstRow ? (['top'] as const) : [])
              ]}
              isToday={isToday}
              isFuture={isFuture}
            >
              {item.backupCare && (
                <StaticChip color={theme.colors.main.m1}>v</StaticChip>
              )}
              <Link
                to={`/child-information/${item.childId}`}
                data-qa="child-name"
              >
                {formatName(
                  item.childFirstName,
                  item.childLastName,
                  i18n,
                  true
                )}
              </Link>
            </AttendanceReservationReportTd>
            {item.fullDayAbsence ? (
              <AttendanceReservationReportTd
                borderEdge={[
                  'right',
                  ...(isToday && isFirstRow ? (['top'] as const) : [])
                ]}
                isToday={isToday}
                isFuture={isFuture}
                colSpan={2}
              >
                {i18n.reports.attendanceReservationByChild.absence}
              </AttendanceReservationReportTd>
            ) : item.reservation ? (
              <>
                <AttendanceReservationReportTd
                  borderEdge={isToday && isFirstRow ? ['top'] : []}
                  isToday={isToday}
                  isFuture={isFuture}
                  data-qa="attendance-reservation-start"
                >
                  {item.reservation.start.format()}
                </AttendanceReservationReportTd>
                <AttendanceReservationReportTd
                  borderEdge={[
                    'right',
                    ...(isToday && isFirstRow ? (['top'] as const) : [])
                  ]}
                  isToday={isToday}
                  isFuture={isFuture}
                  data-qa="attendance-reservation-end"
                >
                  {item.reservation.end.format()}
                </AttendanceReservationReportTd>
              </>
            ) : (
              <AttendanceReservationReportTd
                borderEdge={[
                  'right',
                  ...(isToday && isFirstRow ? (['top'] as const) : [])
                ]}
                isToday={isToday}
                isFuture={isFuture}
                colSpan={2}
              >
                {i18n.reports.attendanceReservationByChild.noReservation}
              </AttendanceReservationReportTd>
            )}
          </React.Fragment>
        )
      })}
    </Tr>
  )
})
