// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RefObject } from 'react'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { ReservationType } from 'lib-common/generated/api-types/reports'
import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import { formatDecimal } from 'lib-common/utils/number'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'

import type { getAttendanceReservationReportByUnit } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { unitGroupsQuery, daycaresQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { attendanceReservationReportByUnitQuery } from './queries'

interface Form {
  range: FiniteDateRange
  unitId: DaycareId | null
  groupIds: GroupId[]
  reservationType: ReservationType
}

const initialState = (): Form => {
  const defaultDate = LocalDate.todayInSystemTz().addWeeks(1)
  return {
    range: new FiniteDateRange(
      defaultDate.startOfWeek(),
      defaultDate.endOfWeek()
    ),
    unitId: null,
    groupIds: [],
    reservationType: 'RESERVATION'
  }
}

const dateFormat = 'EEEEEE d.M.'
const timeFormat = 'HH:mm'

interface AttendanceReservationReportUiRow {
  capacityFactor: string
  childCount: number
  childCountOver3: number
  childCountUnder3: number
  dateTime: HelsinkiDateTime
  groupId: GroupId | null
  groupName: string | null
  staffCount: string
}

export default React.memo(function AttendanceReservation() {
  const { lang, i18n } = useTranslation()

  const [form, setForm] = useState<Form>(initialState)
  const tooLongRange = form.range.end.isAfter(form.range.start.addMonths(2))
  const isValid = !tooLongRange && form.unitId !== null

  const units = useQueryResult(daycaresQuery({ includeClosed: true }))
  const groups = useQueryResult(
    form.unitId
      ? unitGroupsQuery({ daycareId: form.unitId })
      : constantQuery([])
  )

  const [filters, setFilters] =
    useState<Arg0<typeof getAttendanceReservationReportByUnit>>()
  const report = useQueryResult(
    filters !== undefined
      ? attendanceReservationReportByUnitQuery(filters)
      : constantQuery([])
  )

  const autoScrollRef = useRef<HTMLTableRowElement>(null)

  const fetchAttendanceReservationReport = useCallback(() => {
    setFilters({
      unitId: form.unitId!,
      start: form.range.start,
      end: form.range.end,
      groupIds: form.groupIds,
      reservationType: form.reservationType
    })
  }, [
    form.groupIds,
    form.range.end,
    form.range.start,
    form.reservationType,
    form.unitId
  ])

  useEffect(() => {
    scrollRefIntoView(autoScrollRef)
  }, [report])

  const filteredRows = useMemo(
    () =>
      report.map<AttendanceReservationReportUiRow[]>((data) =>
        data.map((row) => ({
          ...row,
          capacityFactor: formatDecimal(row.capacityFactor),
          staffCount: formatDecimal(row.staffCount)
        }))
      ),
    [report]
  )

  const filteredUnits = units
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])

  const filteredGroups = groups
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])

  const tableComponents = useMemo(
    () =>
      filteredRows.map((filteredRows) => {
        const dates = filteredRows
          .map((row) => row.dateTime.toLocalDate().format(dateFormat, lang))
          .filter((value, index, array) => array.indexOf(value) === index)

        const rowsByGroupAndTime = filteredRows.reduce<
          Record<string, Map<string, AttendanceReservationReportUiRow[]>>
        >((data, row) => {
          const groupKey = row.groupId ?? 'ungrouped'
          const map = data[groupKey] ?? new Map()
          const time = row.dateTime.toLocalTime().format(timeFormat)
          const rows = map.get(time) ?? []
          rows.push(row)
          map.set(time, rows)
          data[groupKey] = map
          return data
        }, {})
        const entries = Object.entries(rowsByGroupAndTime)
        const showGroupTitle = entries.length > 1

        const tableComponents = entries.map(([groupId, rowsByTime]) => {
          const groupName = showGroupTitle
            ? (filteredGroups.find((group) => group.id === groupId)?.name ??
              i18n.reports.attendanceReservation.ungrouped)
            : undefined
          return (
            <TableScrollable
              key={groupId}
              data-qa="report-attendance-reservation-table"
            >
              <Thead sticky>
                <Tr>
                  <Th>{groupName}</Th>
                  {dates.map((date) => (
                    <Th key={date} colSpan={5} align="center">
                      {date}
                    </Th>
                  ))}
                </Tr>
                <Tr>
                  <Th stickyColumn>{i18n.reports.common.clock}</Th>
                  {dates.map((date) => (
                    <React.Fragment key={date}>
                      <Th>{i18n.reports.common.under3y}</Th>
                      <Th>{i18n.reports.common.over3y}</Th>
                      <Th>{i18n.reports.common.totalShort}</Th>
                      <Th>
                        {i18n.reports.attendanceReservation.capacityFactor}
                      </Th>
                      <Th>{i18n.reports.attendanceReservation.staffCount}</Th>
                    </React.Fragment>
                  ))}
                </Tr>
              </Thead>
              <Tbody>{getTableBody(rowsByTime, autoScrollRef)}</Tbody>
            </TableScrollable>
          )
        })
        return tableComponents
      }),
    [filteredGroups, filteredRows, i18n, lang]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.attendanceReservation.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DateRangePicker
              start={form.range.start}
              end={form.range.end}
              onChange={(start, end) => {
                if (start !== null && end !== null) {
                  setForm({
                    ...form,
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
              items={filteredUnits}
              onChange={(selectedItem) =>
                setForm({
                  ...form,
                  unitId: selectedItem !== null ? selectedItem.id : null,
                  groupIds: []
                })
              }
              selectedItem={
                filteredUnits.find((unit) => unit.id === form.unitId) ?? null
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
              options={filteredGroups}
              onChange={(selectedItems) =>
                setForm({
                  ...form,
                  groupIds: selectedItems.map((selectedItem) => selectedItem.id)
                })
              }
              value={filteredGroups.filter((group) =>
                form.groupIds.includes(group.id)
              )}
              getOptionId={(group) => group.id}
              getOptionLabel={(group) => group.name}
              placeholder=""
              isClearable={true}
            />
          </div>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.attendanceType}</FilterLabel>
          <Combobox<ReservationType>
            items={['RESERVATION', 'REALIZATION']}
            onChange={(type) =>
              type ? setForm({ ...form, reservationType: type }) : undefined
            }
            selectedItem={form.reservationType}
            getItemLabel={(item) => i18n.reports.common.attendanceTypes[item]}
          />
        </FilterRow>
        <FilterRow>
          <Button
            primary
            disabled={!isValid}
            text={i18n.common.search}
            onClick={fetchAttendanceReservationReport}
            data-qa="send-button"
          />
        </FilterRow>

        {tooLongRange && (
          <div>{i18n.reports.attendanceReservation.tooLongRange}</div>
        )}
        {filters !== undefined
          ? renderResult(
              combine(filteredRows, tableComponents),
              ([filteredRows, tableComponents]) => (
                <>
                  <ReportDownload
                    data={filteredRows.map((row) => ({
                      ...row,
                      dateTime: row.dateTime.format()
                    }))}
                    columns={[
                      {
                        label: i18n.reports.common.groupName,
                        value: (row) => row.groupName,
                        exclude: filters.groupIds?.length === 0
                      },
                      {
                        label: i18n.reports.common.clock,
                        value: (row) => row.dateTime
                      },
                      {
                        label: i18n.reports.common.under3y,
                        value: (row) => row.childCountUnder3
                      },
                      {
                        label: i18n.reports.common.over3y,
                        value: (row) => row.childCountOver3
                      },
                      {
                        label: i18n.reports.common.totalShort,
                        value: (row) => row.childCount
                      },
                      {
                        label:
                          i18n.reports.attendanceReservation.capacityFactor,
                        value: (row) => row.capacityFactor
                      },
                      {
                        label: i18n.reports.attendanceReservation.staffCount,
                        value: (row) => row.staffCount
                      }
                    ]}
                    filename={`${i18n.reports.attendanceReservation.title} ${
                      filteredUnits.find((unit) => unit.id === filters.unitId)
                        ?.name ?? ''
                    } ${filters.start.formatIso()}-${filters.end.formatIso()}.csv`}
                  />
                  {tableComponents}
                </>
              )
            )
          : null}
      </ContentArea>
    </Container>
  )
})

const getTableBody = (
  rowsByTime: Map<string, AttendanceReservationReportUiRow[]>,
  autoScrollRef: RefObject<HTMLTableRowElement | null>
) => {
  const components: React.ReactNode[] = []
  rowsByTime.forEach((rows, time) => {
    components.push(
      <Tr key={time} ref={time === '05:30' ? autoScrollRef : undefined}>
        <Td sticky>{time}</Td>
        {rows.map((row) => {
          const isToday = row.dateTime.toLocalDate().isToday()
          const isFuture = row.dateTime.isAfter(HelsinkiDateTime.now())
          const firstRow = time === '00:00'
          return (
            <React.Fragment key={row.dateTime.formatIso()}>
              <AttendanceReservationReportTd
                borderEdge={[
                  'left',
                  ...(isToday && firstRow ? (['top'] as const) : [])
                ]}
                isToday={isToday}
                isFuture={isFuture}
              >
                {row.childCountUnder3}
              </AttendanceReservationReportTd>
              <AttendanceReservationReportTd
                borderEdge={isToday && firstRow ? ['top'] : []}
                isToday={isToday}
                isFuture={isFuture}
              >
                {row.childCountOver3}
              </AttendanceReservationReportTd>
              <AttendanceReservationReportTd
                borderEdge={isToday && firstRow ? ['top'] : []}
                isToday={isToday}
                isFuture={isFuture}
              >
                {row.childCount}
              </AttendanceReservationReportTd>
              <AttendanceReservationReportTd
                borderEdge={isToday && firstRow ? ['top'] : []}
                isToday={isToday}
                isFuture={isFuture}
              >
                {row.capacityFactor}
              </AttendanceReservationReportTd>
              <AttendanceReservationReportTd
                borderEdge={[
                  'right',
                  ...(isToday && firstRow ? (['top'] as const) : [])
                ]}
                isToday={isToday}
                isFuture={isFuture}
              >
                {row.staffCount}
              </AttendanceReservationReportTd>
            </React.Fragment>
          )
        })}
      </Tr>
    )
  })
  return components
}

export interface AttendanceReservationReportTdProps {
  borderEdge: ('top' | 'left' | 'right')[]
  isToday: boolean
  isFuture: boolean
}

export const AttendanceReservationReportTd = styled(
  Td
)<AttendanceReservationReportTdProps>`
  text-align: center;
  ${(props) =>
    props.borderEdge.includes('top')
      ? `border-top: ${props.isToday ? '2px' : '1px'} solid ${
          props.isToday
            ? props.theme.colors.status.success
            : props.theme.colors.grayscale.g15
        };`
      : ''}
  ${(props) =>
    props.borderEdge.includes('right')
      ? `border-right: ${props.isToday ? '2px' : '1px'} solid ${
          props.isToday
            ? props.theme.colors.status.success
            : props.theme.colors.grayscale.g15
        };`
      : ''}
  ${(props) =>
    props.borderEdge.includes('left')
      ? `border-left: ${props.isToday ? '2px' : '1px'} solid ${
          props.isToday
            ? props.theme.colors.status.success
            : props.theme.colors.grayscale.g15
        };`
      : ''}
  ${(props) =>
    props.isFuture ? `color: ${props.theme.colors.grayscale.g70};` : ''}
`
