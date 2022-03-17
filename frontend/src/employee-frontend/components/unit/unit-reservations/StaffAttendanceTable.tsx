// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import { isSameDay } from 'date-fns'
import { sortBy } from 'lodash'
import React, { useMemo } from 'react'
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
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'

import {
  AttendanceTableHeader,
  DayTd,
  DayTr,
  NameTd,
  NameWrapper
} from './attendance-elements'
import { StyledTd } from './attendance-elements'

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
        staffAttendances,
        (attendance) => attendance.lastName,
        (attendance) => attendance.firstName
      ),
    [staffAttendances]
  )

  const extraRows = useMemo(
    () => sortBy(extraAttendances, (attendance) => attendance.name),
    [extraAttendances]
  )

  return (
    <Table>
      <AttendanceTableHeader operationalDays={operationalDays} />
      <Tbody>
        {staffRows.flatMap((employeeAttendance, index) => {
          const { firstName, lastName, employeeId, attendances } =
            employeeAttendance
          return (
            <AttendanceRow
              key={employeeId}
              id={`${employeeId}-${index}`}
              dataQa={`staff-attendance-row-${employeeId}`}
              rowIndex={index}
              name={formatName(firstName.split(/\s/)[0], lastName, i18n, true)}
              operationalDays={operationalDays}
              attendances={attendances}
            />
          )
        })}
        {extraRows.flatMap((externalAttendance, index) => {
          const { name, id, arrived } = externalAttendance
          return (
            <AttendanceRow
              key={id}
              id={`${id}-${index}`}
              dataQa={`staff-attendance-row-${id}`}
              rowIndex={index}
              name={name}
              operationalDays={operationalDays}
              attendances={[{ id: `${name}-${index}`, arrived }]}
            />
          )
        })}
      </Tbody>
    </Table>
  )
})

type AttendanceRowProps = {
  id: string
  dataQa: string
  rowIndex: number
  name: string
  operationalDays: OperationalDay[]
  attendances: Array<{ id: string; arrived: Date; departed?: Date | null }>
}
const AttendanceRow = React.memo(function AttendanceRow({
  id,
  dataQa,
  rowIndex,
  name,
  operationalDays,
  attendances
}: AttendanceRowProps) {
  return (
    <DayTr key={id} data-qa={dataQa}>
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
          {attendances?.length > 0 ? (
            attendances.map((attendance) => (
              <AttendanceCell key={attendance.id}>
                <AttendanceTime>
                  {isSameDay(attendance.arrived, day.date.toSystemTzDate())
                    ? formatTime(attendance.arrived)
                    : '–'}
                </AttendanceTime>
                <AttendanceTime>
                  {attendance.departed &&
                  isSameDay(attendance.departed, day.date.toSystemTzDate())
                    ? formatTime(attendance.departed)
                    : '–'}
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
