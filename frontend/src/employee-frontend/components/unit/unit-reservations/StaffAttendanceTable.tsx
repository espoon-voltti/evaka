// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import { isSameDay } from 'date-fns'
import { groupBy, sortBy } from 'lodash'
import React, { useMemo, useState, useEffect } from 'react'
import styled from 'styled-components'

import EllipsisMenu from 'employee-frontend/components/common/EllipsisMenu'
import { Result, Success } from 'lib-common/api'
import { OperationalDay } from 'lib-common/api-types/reservations'
import { formatTime } from 'lib-common/date'
import {
  Attendance,
  EmployeeAttendance,
  ExternalAttendance,
  UpsertStaffAttendanceRequest,
  UpsertExternalAttendanceRequest
} from 'lib-common/generated/api-types/attendance'
import { TimeRange } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { validateTimeRange } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Table, Tbody } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'

import {
  AttendanceTableHeader,
  DayTd,
  DayTr,
  EditStateIndicator,
  NameTd,
  NameWrapper,
  StyledTd,
  TimeRangeEditor
} from './attendance-elements'
import { TimeRangeWithErrors } from './reservation-table-edit-state'

interface Props {
  operationalDays: OperationalDay[]
  staffAttendances: EmployeeAttendance[]
  extraAttendances: ExternalAttendance[]
  saveAttendance: (body: UpsertStaffAttendanceRequest) => Promise<Result<void>>
  saveExternalAttendance: (
    body: UpsertExternalAttendanceRequest
  ) => Promise<Result<void>>
  enableNewEntries?: boolean
  reloadStaffAttendances: () => void
}

export default React.memo(function StaffAttendanceTable({
  staffAttendances,
  extraAttendances,
  operationalDays,
  saveAttendance,
  saveExternalAttendance,
  enableNewEntries,
  reloadStaffAttendances
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
          ([name, attendances]) => ({
            name,
            attendances
          })
        ),
        (attendance) => attendance.name
      ),
    [extraAttendances]
  )

  return (
    <Table>
      <AttendanceTableHeader
        operationalDays={operationalDays}
        startTimeHeader={i18n.unit.staffAttendance.startTime}
        endTimeHeader={i18n.unit.staffAttendance.endTime}
      />
      <Tbody>
        {staffRows.map((row, index) => (
          <AttendanceRow
            key={`${row.employeeId}-${index}`}
            rowIndex={index}
            isPositiveOccupancyCoefficient={row.currentOccupancyCoefficient > 0}
            name={row.name}
            employeeId={row.employeeId}
            operationalDays={operationalDays}
            attendances={row.attendances}
            saveAttendance={saveAttendance}
            enableNewEntries={enableNewEntries}
            reloadStaffAttendances={reloadStaffAttendances}
          />
        ))}
        {extraRowsGroupedByName.map((row, index) => (
          <AttendanceRow
            key={`${row.name}-${index}`}
            rowIndex={index}
            isPositiveOccupancyCoefficient={
              row.attendances[0].occupancyCoefficient > 0
            }
            name={row.name}
            operationalDays={operationalDays}
            attendances={row.attendances}
            saveExternalAttendance={saveExternalAttendance}
            enableNewEntries={enableNewEntries}
            reloadStaffAttendances={reloadStaffAttendances}
          />
        ))}
      </Tbody>
    </Table>
  )
})

type TimeRangeWithErrorsAndIds = TimeRangeWithErrors & {
  id?: UUID
  groupId?: UUID
}
const emptyTimeRange: TimeRangeWithErrorsAndIds = {
  startTime: '',
  endTime: '',
  errors: {
    startTime: undefined,
    endTime: undefined
  },
  lastSavedState: {
    startTime: '',
    endTime: ''
  },
  id: undefined,
  groupId: undefined
}

interface AttendanceRowProps extends BaseProps {
  rowIndex: number
  isPositiveOccupancyCoefficient: boolean
  name: string
  employeeId?: string
  operationalDays: OperationalDay[]
  attendances: Attendance[]
  saveAttendance?: (body: UpsertStaffAttendanceRequest) => Promise<Result<void>>
  saveExternalAttendance?: (
    body: UpsertExternalAttendanceRequest
  ) => Promise<Result<void>>
  enableNewEntries?: boolean
  reloadStaffAttendances: () => void
}

const AttendanceRow = React.memo(function AttendanceRow({
  rowIndex,
  isPositiveOccupancyCoefficient,
  name,
  employeeId,
  operationalDays,
  attendances,
  saveAttendance,
  saveExternalAttendance,
  enableNewEntries,
  reloadStaffAttendances
}: AttendanceRowProps) {
  const { i18n } = useTranslation()
  const [editing, setEditing] = useState<boolean>(false)
  const [values, setValues] = useState<
    Array<{ date: LocalDate; timeRanges: TimeRangeWithErrorsAndIds[] }>
  >([])
  const [dirtyDates, setDirtyDates] = useState<Set<LocalDate>>(
    new Set<LocalDate>()
  )

  useEffect(
    () =>
      setValues(
        operationalDays.map((day) => {
          const attendancesOnDay = attendances.filter((a) =>
            isSameDay(a.arrived, day.date.toSystemTzDate())
          )
          return {
            date: day.date,
            timeRanges:
              attendancesOnDay.length > 0
                ? attendancesOnDay.map((attendance) => {
                    const startTime = formatTime(attendance.arrived)
                    const endTime = attendance.departed
                      ? formatTime(attendance.departed)
                      : ''
                    return {
                      id: attendance.id,
                      groupId: attendance.groupId,
                      startTime,
                      endTime,
                      errors: {
                        startTime: undefined,
                        endTime: undefined
                      },
                      lastSavedState: {
                        startTime,
                        endTime
                      }
                    }
                  })
                : [emptyTimeRange]
          }
        })
      ),
    [operationalDays, attendances]
  )

  const updateValue = (
    date: LocalDate,
    rangeIx: number,
    updatedRange: TimeRange
  ) => {
    setValues(
      values.map((val) => {
        return val.date === date
          ? {
              ...val,
              timeRanges: val.timeRanges.map((range, ix) => {
                return ix === rangeIx
                  ? {
                      ...range,
                      ...updatedRange,
                      errors: validateTimeRange(updatedRange)
                    }
                  : range
              })
            }
          : val
      })
    )
  }

  return (
    <DayTr>
      <NameTd partialRow={false} rowIndex={rowIndex}>
        <FixedSpaceRow spacing="xs">
          <Tooltip
            tooltip={
              isPositiveOccupancyCoefficient
                ? i18n.unit.attendanceReservations.affectsOccupancy
                : i18n.unit.attendanceReservations.doesNotAffectOccupancy
            }
            position="bottom"
            width="large"
          >
            <RoundIcon
              content="K"
              active={isPositiveOccupancyCoefficient}
              color={colors.accents.a3emerald}
              size="s"
            />
          </Tooltip>
          <NameWrapper>{name}</NameWrapper>
        </FixedSpaceRow>
      </NameTd>
      {values.map(({ date, timeRanges }) => (
        <DayTd
          key={date.formatIso()}
          className={classNames({ 'is-today': date.isToday() })}
          partialRow={false}
          rowIndex={rowIndex}
        >
          {timeRanges.map((range, rangeIx) => (
            <AttendanceCell key={`${date.formatIso()}-${rangeIx}`}>
              {editing && (range.id || enableNewEntries) ? (
                <TimeRangeEditor
                  timeRange={range}
                  update={(updatedValue) =>
                    updateValue(date, rangeIx, updatedValue)
                  }
                  save={() => {
                    setDirtyDates(dirtyDates.add(date))
                  }}
                />
              ) : (
                <>
                  <AttendanceTime>
                    {range.startTime === '' ? '–' : range.startTime}
                  </AttendanceTime>
                  <AttendanceTime>
                    {range.endTime === '' ? '–' : range.endTime}
                  </AttendanceTime>
                </>
              )}
            </AttendanceCell>
          ))}
        </DayTd>
      ))}
      <StyledTd partialRow={false} rowIndex={rowIndex} rowSpan={1}>
        {editing ? (
          <EditStateIndicator
            status={Success.of()}
            stopEditing={() => {
              setEditing(false)
              const savePromises = values.flatMap(({ date, timeRanges }) =>
                timeRanges.map((range) => {
                  if (dirtyDates.has(date) && range !== emptyTimeRange) {
                    const commonParams = {
                      attendanceId: range.id || null,
                      arrived: date.toSystemTzDateAtTime(range.startTime),
                      departed:
                        range.endTime !== ''
                          ? date.toSystemTzDateAtTime(range.endTime)
                          : null,
                      groupId: range.groupId || ''
                    }
                    if (saveAttendance && employeeId) {
                      return saveAttendance({
                        ...commonParams,
                        employeeId: employeeId
                      })
                    } else if (saveExternalAttendance) {
                      return saveExternalAttendance({
                        ...commonParams,
                        name: name
                      })
                    }
                  }
                  return Promise.resolve()
                })
              )
              setDirtyDates(new Set<LocalDate>())
              return Promise.all(savePromises).then(() =>
                reloadStaffAttendances()
              )
            }}
          />
        ) : (
          <RowMenu onEdit={() => setEditing(true)} />
        )}
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
  flex: 1 0 54px;
  text-align: center;
  white-space: nowrap;
`

type RowMenuProps = {
  onEdit: () => void
}
const RowMenu = React.memo(function RowMenu({ onEdit }: RowMenuProps) {
  const { i18n } = useTranslation()
  return (
    <EllipsisMenu
      items={[
        {
          id: 'edit-row',
          label: i18n.unit.attendanceReservations.editRow,
          onClick: onEdit
        }
      ]}
    />
  )
})
