// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RefObject } from 'react'
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { localDateRange } from 'lib-common/form/fields'
import {
  array,
  object,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import type { StateOf } from 'lib-common/form/types'
import type {
  Daycare,
  DaycareGroup
} from 'lib-common/generated/api-types/daycare'
import type { ReservationType } from 'lib-common/generated/api-types/reports'
import type { GroupId } from 'lib-common/generated/api-types/shared'
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
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'

import type { getAttendanceReservationReportByUnit } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { unitGroupsQuery, daycaresQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { attendanceReservationReportByUnitQuery } from './queries'

const model = transformed(
  object({
    range: required(localDateRange()),
    unit: required(value<Daycare | undefined>()),
    groups: array(value<DaycareGroup>()),
    reservationType: value<ReservationType>()
  }),
  (output) => {
    const tooLongRange = output.range.end.isAfter(
      output.range.start.addMonths(2)
    )
    if (tooLongRange) {
      return ValidationError.field('range', 'tooLongRange')
    }
    return ValidationSuccess.of({
      start: output.range.start,
      end: output.range.end,
      unitId: output.unit.id,
      groupIds: output.groups.map((group) => group.id),
      reservationType: output.reservationType
    })
  }
)
const initialState = (): StateOf<typeof model> => {
  const defaultDate = LocalDate.todayInSystemTz().addWeeks(1)
  return {
    range: localDateRange.fromRange(
      new FiniteDateRange(defaultDate.startOfWeek(), defaultDate.endOfWeek())
    ),
    unit: undefined,
    groups: [],
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

  const form = useForm(model, initialState, {
    ...i18n.validationErrors,
    tooLongRange: i18n.reports.attendanceReservation.tooLongRange
  })
  const {
    range: rangeField,
    unit: unitField,
    groups: groupsField,
    reservationType: reservationTypeField
  } = useFormFields(form)

  const units = useQueryResult(daycaresQuery({ includeClosed: true }))
  const groups = useQueryResult(
    unitField.isValid()
      ? unitGroupsQuery({ daycareId: unitField.value().id })
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
    setFilters(form.value())
  }, [form])

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
            <DateRangePickerF bind={rangeField} locale={lang} />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <FlexRow>
            <Combobox
              items={filteredUnits}
              onChange={(selectedItem) =>
                unitField.set(selectedItem ?? undefined)
              }
              selectedItem={unitField.state ?? null}
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
              onChange={groupsField.set}
              value={groupsField.state}
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
              type ? reservationTypeField.set(type) : undefined
            }
            selectedItem={reservationTypeField.state}
            getItemLabel={(item) => i18n.reports.common.attendanceTypes[item]}
          />
        </FilterRow>
        <FilterRow>
          <Button
            primary
            disabled={!form.isValid()}
            text={i18n.common.search}
            onClick={fetchAttendanceReservationReport}
            data-qa="send-button"
          />
        </FilterRow>

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
