// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import { isSameDay } from 'date-fns'
import { groupBy, sortBy } from 'lodash'
import React, { useMemo, useCallback } from 'react'
import styled from 'styled-components'

import EllipsisMenu from 'employee-frontend/components/common/EllipsisMenu'
import { OperationalDay } from 'lib-common/api-types/reservations'
import { formatTime } from 'lib-common/date'
import {
  EmployeeAttendance,
  ExternalAttendance
} from 'lib-common/generated/api-types/attendance'
import { Table, Tbody } from 'lib-components/layout/Table'
import { fontWeights } from 'lib-components/typography'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'

import {
  AttendanceTableHeader,
  DayTd,
  DayTr,
  NameTd,
  NameWrapper,
  StyledTd
} from './attendance-elements'

interface Props {
  operationalDays: OperationalDay[]
  staffAttendances: EmployeeAttendance[]
  extraAttendances: ExternalAttendance[]
}

export default React.memo(function StaffAttendanceTable({
  staffAttendances,
  extraAttendances,
  operationalDays
}: Props) {
  const { i18n } = useTranslation()

  const staffRows = useMemo(
    () =>
      sortBy(
        staffAttendances.map(({ firstName, lastName, ...rest }) => ({
          ...rest,
          name: formatName(firstName.split(/\s/)[0], lastName, i18n, true)
        })),
        (attendance) => attendance.name
      ),
    [i18n, staffAttendances]
  )

  const extraRowsGroupedByName = useMemo(
    () =>
      sortBy(
        Object.entries(groupBy(extraAttendances, (a) => a.name)).map(
          ([name, rows]) => ({
            name,
            attendances: rows.map(({ id, arrived, departed }) => ({
              id,
              arrived,
              departed
            }))
          })
        ),
        (attendance) => attendance.name
      ),
    [extraAttendances]
  )

  return (
    <Table>
      <AttendanceTableHeader operationalDays={operationalDays} />
      <Tbody>
        {staffRows.map(({ attendances, employeeId, name }, index) => (
          <AttendanceRow
            key={`${employeeId}-${index}`}
            rowIndex={index}
            name={name}
            operationalDays={operationalDays}
            attendances={attendances}
          />
        ))}
        {extraRowsGroupedByName.map((row, index) => (
          <AttendanceRow
            key={`${row.name}-${index}`}
            rowIndex={index}
            name={row.name}
            operationalDays={operationalDays}
            attendances={row.attendances}
          />
        ))}
      </Tbody>
    </Table>
  )
})

interface AttendanceRowProps extends BaseProps {
  rowIndex: number
  name: string
  operationalDays: OperationalDay[]
  attendances: { id: string; arrived: Date; departed?: Date | null }[]
}

const AttendanceRow = React.memo(function AttendanceRow({
  rowIndex,
  name,
  operationalDays,
  attendances
}: AttendanceRowProps) {
  const attendancesForDay = useCallback(
    (date: Date) =>
      sortBy(
        attendances.filter((attendance) => isSameDay(attendance.arrived, date)),
        (attendance) => attendance.arrived
      ),
    [attendances]
  )
  return (
    <DayTr>
      <NameTd partialRow={false} rowIndex={rowIndex}>
        <NameWrapper>{name}</NameWrapper>
      </NameTd>
      {operationalDays.map((day) => (
        <DayTd
          key={day.date.formatIso()}
          className={classNames({ 'is-today': day.date.isToday() })}
          partialRow={false}
          rowIndex={rowIndex}
        >
          {attendancesForDay(day.date.toSystemTzDate())?.length > 0 ? (
            attendancesForDay(day.date.toSystemTzDate()).map((attendance) => (
              <AttendanceCell key={attendance.id}>
                <AttendanceTime>
                  {formatTime(attendance.arrived)}
                </AttendanceTime>
                <AttendanceTime>
                  {attendance.departed ? formatTime(attendance.departed) : '–'}
                </AttendanceTime>
              </AttendanceCell>
            ))
          ) : (
            <AttendanceCell>
              <AttendanceTime>–</AttendanceTime>
              <AttendanceTime>–</AttendanceTime>
            </AttendanceCell>
          )}
        </DayTd>
      ))}
      <StyledTd partialRow={false} rowIndex={rowIndex} rowSpan={1}>
        <RowMenu />
      </StyledTd>
    </DayTr>
  )
})

const AttendanceCell = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-evenly;
  padding: ${defaultMargins.xs};
  gap: ${defaultMargins.xs};
`

const AttendanceTime = styled.span`
  font-weight: ${fontWeights.semibold};
`

const RowMenu = React.memo(function RowMenu() {
  return (
    <EllipsisMenu
      items={[
        {
          id: 'edit-row',
          label: 'TODO: edit',
          onClick: () => {
            // TODO
          }
        }
      ]}
    />
  )
})
