// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import { isSameDay } from 'date-fns'
import { groupBy, initial, last, sortBy } from 'lodash'
import React, { useMemo, useState, useEffect, useCallback } from 'react'
import styled from 'styled-components'

import EllipsisMenu from 'employee-frontend/components/common/EllipsisMenu'
import { Result, Success } from 'lib-common/api'
import { OperationalDay } from 'lib-common/api-types/reservations'
import { formatTime } from 'lib-common/date'
import {
  Attendance,
  EmployeeAttendance,
  ExternalAttendance,
  UpsertStaffAndExternalAttendanceRequest
} from 'lib-common/generated/api-types/attendance'
import { TimeRange } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { validateTimeRange } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { faClock } from 'lib-icons'

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
  TimeInputWithoutPadding
} from './attendance-elements'
import { TimeRangeWithErrors } from './reservation-table-edit-state'

interface Props {
  operationalDays: OperationalDay[]
  staffAttendances: EmployeeAttendance[]
  extraAttendances: ExternalAttendance[]
  saveAttendances: (
    body: UpsertStaffAndExternalAttendanceRequest
  ) => Promise<Result<void>>
  enableNewEntries?: boolean
  reloadStaffAttendances: () => void
}

export default React.memo(function StaffAttendanceTable({
  staffAttendances,
  extraAttendances,
  operationalDays,
  saveAttendances,
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
    <Table data-qa="staff-attendances-table">
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
            saveAttendances={saveAttendances}
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
            saveAttendances={saveAttendances}
            enableNewEntries={enableNewEntries}
            reloadStaffAttendances={reloadStaffAttendances}
          />
        ))}
      </Tbody>
    </Table>
  )
})

type TimeRangeWithErrorsAndMetadata = TimeRangeWithErrors & {
  id?: UUID
  groupId?: UUID
  arrived: LocalDate
  departed: LocalDate
}
const emptyTimeRangeAt = (date: LocalDate): TimeRangeWithErrorsAndMetadata => ({
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
  groupId: undefined,
  arrived: date,
  departed: date
})

const OvernightAwareTimeRangeEditor = React.memo(
  function OvernightAwareTimeRangeEditor({
    timeRange,
    update,
    save
  }: {
    timeRange: TimeRangeWithErrors
    update: (v: TimeRange) => void
    save: () => void
  }) {
    const { startTime, endTime, errors } = timeRange
    const [editingArrival, setEditingArrival] = useState<boolean>(
      startTime !== '00:00'
    )
    const [editingDeparture, setEditingDeparture] = useState<boolean>(
      endTime !== '23:59'
    )
    const editArrival = useCallback(() => setEditingArrival(true), [])
    const editDeparture = useCallback(() => setEditingDeparture(true), [])

    return (
      <>
        {editingArrival ? (
          <TimeInputWithoutPadding
            value={startTime}
            onChange={(value) => update({ startTime: value, endTime })}
            onBlur={save}
            info={
              errors.startTime ? { status: 'warning', text: '' } : undefined
            }
            data-qa="input-start-time"
          />
        ) : (
          <IconButton icon={faClock} onClick={editArrival} />
        )}
        {editingDeparture ? (
          <TimeInputWithoutPadding
            value={endTime}
            onChange={(value) => update({ startTime, endTime: value })}
            onBlur={save}
            info={errors.endTime ? { status: 'warning', text: '' } : undefined}
            data-qa="input-end-time"
          />
        ) : (
          <IconButton icon={faClock} onClick={editDeparture} />
        )}
      </>
    )
  }
)

interface AttendanceRowProps extends BaseProps {
  rowIndex: number
  isPositiveOccupancyCoefficient: boolean
  name: string
  employeeId?: string
  operationalDays: OperationalDay[]
  attendances: Attendance[]
  saveAttendances: (
    body: UpsertStaffAndExternalAttendanceRequest
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
  saveAttendances,
  enableNewEntries,
  reloadStaffAttendances
}: AttendanceRowProps) {
  const { i18n } = useTranslation()
  const [editing, setEditing] = useState<boolean>(false)
  const [values, setValues] = useState<
    Array<{ date: LocalDate; timeRanges: TimeRangeWithErrorsAndMetadata[] }>
  >([])
  const [dirtyDates, setDirtyDates] = useState<Set<LocalDate>>(
    new Set<LocalDate>()
  )

  useEffect(() => {
    // Splits overnight attendances to separate days
    const dailyAttendances = attendances.flatMap<Attendance>((attendance) => {
      if (
        attendance.departed &&
        !isSameDay(attendance.arrived, attendance.departed)
      ) {
        return [
          {
            ...attendance,
            departed: LocalDate.fromSystemTzDate(
              attendance.arrived
            ).toSystemTzDateAtTime('23:59')
          },
          {
            ...attendance,
            id: '',
            arrived: LocalDate.fromSystemTzDate(
              attendance.departed
            ).toSystemTzDateAtTime('00:00')
          }
        ]
      }
      return attendance
    })

    const daysAndRanges = operationalDays.map((day) => {
      const attendancesOnDay = dailyAttendances.filter((a) =>
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
                  arrived: day.date,
                  departed: day.date,
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
            : [emptyTimeRangeAt(day.date)]
      }
    })
    setValues(daysAndRanges)
  }, [operationalDays, attendances])

  const updateValue = useCallback(
    (date: LocalDate, rangeIx: number, updatedRange: TimeRange) => {
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
    },
    [setValues, values]
  )

  const saveChanges = useCallback(() => {
    setEditing(false)

    const rangeToUpsertParams = (range: TimeRangeWithErrorsAndMetadata) => ({
      attendanceId: !range.id || range.id === '' ? null : range.id,
      arrived: range.arrived.toSystemTzDateAtTime(range.startTime),
      departed:
        range.endTime !== ''
          ? range.departed.toSystemTzDateAtTime(range.endTime)
          : null,
      groupId: range.groupId || ''
    })

    const ranges = values
      .flatMap(({ timeRanges }) => timeRanges)
      .filter((r) => !(r.startTime === '' && r.endTime === ''))
      .reduce((ranges, r) => {
        const prevRange = last(ranges)
        if (prevRange && prevRange.endTime === '' && r.startTime === '') {
          // This is an overnight entry. Merge with the previous day for saving
          return [
            ...initial(ranges),
            {
              ...prevRange,
              departed: r.departed,
              endTime: r.endTime
            }
          ]
        }
        return [...ranges, r]
      }, [] as TimeRangeWithErrorsAndMetadata[])
      .map(rangeToUpsertParams)

    return saveAttendances({
      staffAttendances: employeeId
        ? ranges.map((r) => ({ ...r, employeeId }))
        : [],
      externalAttendances: !employeeId
        ? ranges.map((r) => ({ ...r, name }))
        : []
    }).then(() => reloadStaffAttendances())
  }, [
    setEditing,
    values,
    saveAttendances,
    reloadStaffAttendances,
    employeeId,
    name
  ])

  const renderTime = useCallback((time: string) => {
    switch (time) {
      case '':
        return '-'
      case '00:00':
      case '23:59':
        return '→'
      default:
        return time
    }
  }, [])

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
              data-qa={
                isPositiveOccupancyCoefficient
                  ? 'icon-occupancy-coefficient-pos'
                  : 'icon-occupancy-coefficient'
              }
            />
          </Tooltip>
          <NameWrapper data-qa="staff-attendance-name">{name}</NameWrapper>
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
                <OvernightAwareTimeRangeEditor
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
                  <AttendanceTime>{renderTime(range.startTime)}</AttendanceTime>
                  <AttendanceTime>{renderTime(range.endTime)}</AttendanceTime>
                </>
              )}
            </AttendanceCell>
          ))}
        </DayTd>
      ))}
      <StyledTd partialRow={false} rowIndex={rowIndex} rowSpan={1}>
        {editing ? (
          <EditStateIndicator status={Success.of()} stopEditing={saveChanges} />
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
