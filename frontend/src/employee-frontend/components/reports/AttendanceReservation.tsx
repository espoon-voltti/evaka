// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled, { DefaultTheme, useTheme } from 'styled-components'

import { Unit } from 'employee-frontend/api/daycare'
import { getDaycares } from 'employee-frontend/api/unit'
import { Loading, Result } from 'lib-common/api'
import { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'

import {
  AttendanceReservationReportFilters,
  getAssistanceReservationReport
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, TableScrollable } from './common'

const dateFormat = 'EEEEEE d.M.'
const timeFormat = 'HH:mm'

export default React.memo(function AttendanceReservation() {
  const { lang, i18n } = useTranslation()
  const theme = useTheme()

  const [unitId, setUnitId] = useState<UUID | null>(null)
  const [filters, setFilters] = useState<AttendanceReservationReportFilters>(
    () => {
      const defaultDate = LocalDate.todayInSystemTz().addWeeks(1)
      return {
        start: defaultDate.startOfWeek(),
        end: defaultDate.endOfWeek()
      }
    }
  )

  const [units, setUnits] = useState<Result<Unit[]>>(Loading.of())
  const loadUnits = useRestApi(getDaycares, setUnits)
  useEffect(() => {
    void loadUnits()
  }, [loadUnits])

  const [report, setReport] = useState<
    Result<AttendanceReservationReportRow[]>
  >(Loading.of())
  const loadReport = useRestApi(getAssistanceReservationReport, setReport)
  useEffect(() => {
    if (unitId !== null) {
      void loadReport(unitId, filters)
    }
  }, [loadReport, unitId, filters])

  const filteredRows = report
    .map<AttendanceReservationReportRow[]>((row) => row)
    .getOrElse<AttendanceReservationReportRow[]>([])
  const filteredUnits = units
    .map((unit) => unit)
    .getOrElse([])
    .sort((a, b) => a.name.localeCompare(b.name, lang))

  const dates = filteredRows
    .map((row) => row.dateTime.toLocalDate().format(dateFormat, lang))
    .filter((value, index, array) => array.indexOf(value) === index)

  const rowsByTime = filteredRows.reduce<
    Map<string, AttendanceReservationReportRow[]>
  >((map, row) => {
    const time = row.dateTime.toLocalTime().format(timeFormat)
    const rows = map.get(time) ?? []
    rows.push(row)
    map.set(time, rows)
    return map
  }, new Map())

  const tableBody = getTableBody(theme, rowsByTime)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.attendanceReservation.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DateRangePicker
              start={filters.start}
              end={filters.end}
              onChange={(start, end) => {
                if (start !== null && end !== null) {
                  setFilters({ ...filters, start, end })
                }
              }}
              locale={lang}
              errorTexts={i18n.validationErrors}
              required={true}
            ></DateRangePicker>
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <FlexRow>
            <Combobox
              items={filteredUnits}
              onChange={(selectedItem) =>
                setUnitId(selectedItem !== null ? selectedItem.id : null)
              }
              selectedItem={
                filteredUnits.find((unit) => unit.id === unitId) ?? null
              }
              getItemLabel={(item) => item.name}
              placeholder={i18n.filters.unitPlaceholder}
            />
          </FlexRow>
        </FilterRow>

        {unitId !== null && report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <ReportDownload
              data={filteredRows}
              headers={[
                {
                  label: i18n.reports.common.clock,
                  key: 'dateTime'
                },
                {
                  label: i18n.reports.common.under3y,
                  key: 'childCountUnder3'
                },
                {
                  label: i18n.reports.common.over3y,
                  key: 'childCountOver3'
                },
                {
                  label: i18n.reports.common.totalShort,
                  key: 'childCount'
                },
                {
                  label: i18n.reports.attendanceReservation.capacityFactor,
                  key: 'capacityFactor'
                },
                {
                  label: i18n.reports.attendanceReservation.staffCountRequired,
                  key: 'staffCountRequired'
                }
              ]}
              filename={`${i18n.reports.attendanceReservation.title} ${
                filteredUnits.find((unit) => unit.id === unitId)?.name ?? ''
              } ${filters.start.formatIso()}-${filters.end.formatIso()}.csv`}
            />
            <TableScrollable data-qa="report-attendance-reservation-table">
              <Thead sticky>
                <Tr>
                  <Th></Th>
                  {dates.map((date) => {
                    return (
                      <Th key={date} colSpan={5} align="center">
                        {date}
                      </Th>
                    )
                  })}
                </Tr>
                <Tr>
                  <Th stickyColumn>{i18n.reports.common.clock}</Th>
                  {dates.map((date) => {
                    return (
                      <React.Fragment key={date}>
                        <Th>{i18n.reports.common.under3y}</Th>
                        <Th>{i18n.reports.common.over3y}</Th>
                        <Th>{i18n.reports.common.totalShort}</Th>
                        <Th>
                          {i18n.reports.attendanceReservation.capacityFactor}
                        </Th>
                        <Th>
                          {
                            i18n.reports.attendanceReservation
                              .staffCountRequired
                          }
                        </Th>
                      </React.Fragment>
                    )
                  })}
                </Tr>
              </Thead>
              <Tbody>{tableBody}</Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})

const getTableBody = (
  theme: DefaultTheme,
  rowsByTime: Map<string, AttendanceReservationReportRow[]>
) => {
  const components: React.ReactNode[] = []
  rowsByTime.forEach((rows, time) => {
    components.push(
      <Tr key={time}>
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
                {row.staffCountRequired}
              </AttendanceReservationReportTd>
            </React.Fragment>
          )
        })}
      </Tr>
    )
  })
  return components
}

interface AttendanceReservationReportTdProps {
  borderEdge: Array<'top' | 'left' | 'right'>
  isToday: boolean
  isFuture: boolean
}

const AttendanceReservationReportTd = styled(
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
    props.isFuture ? `color: ${props.theme.colors.grayscale.g35};` : ''}
`
