// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import groupBy from 'lodash/groupBy'
import initial from 'lodash/initial'
import last from 'lodash/last'
import partition from 'lodash/partition'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useState, useEffect, useCallback } from 'react'
import styled from 'styled-components'

import { Result, Success } from 'lib-common/api'
import { OperationalDay } from 'lib-common/api-types/reservations'
import {
  Attendance,
  EmployeeAttendance,
  ExternalAttendance,
  StaffAttendanceType,
  UpsertStaffAndExternalAttendanceRequest
} from 'lib-common/generated/api-types/attendance'
import { TimeRange } from 'lib-common/generated/api-types/reservations'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
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
import { featureFlags } from 'lib-customizations/employee'
import { faCircleEllipsis, faClock } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'
import EllipsisMenu from '../../common/EllipsisMenu'

import StaffAttendanceDetailsModal from './StaffAttendanceDetailsModal'
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
  unitId: UUID
  operationalDays: OperationalDay[]
  staffAttendances: EmployeeAttendance[]
  extraAttendances: ExternalAttendance[]
  saveAttendances: (
    body: UpsertStaffAndExternalAttendanceRequest
  ) => Promise<Result<void>>
  deleteAttendances: (
    staffAttendanceIds: UUID[],
    externalStaffAttendanceIds: UUID[]
  ) => Promise<Result<void>[]>
  enableNewEntries?: boolean
  reloadStaffAttendances: () => void
}

export default React.memo(function StaffAttendanceTable({
  unitId,
  staffAttendances,
  extraAttendances,
  operationalDays,
  saveAttendances,
  deleteAttendances,
  enableNewEntries,
  reloadStaffAttendances
}: Props) {
  const { i18n } = useTranslation()
  const [detailsModal, setDetailsModal] = useState<{
    employeeId: string
    date: LocalDate
  }>()
  const closeModal = useCallback(() => setDetailsModal(undefined), [])

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
    <>
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
              isPositiveOccupancyCoefficient={
                row.currentOccupancyCoefficient > 0
              }
              name={row.name}
              employeeId={row.employeeId}
              operationalDays={operationalDays}
              attendances={row.attendances}
              saveAttendances={saveAttendances}
              deleteAttendances={deleteAttendances}
              enableNewEntries={enableNewEntries}
              reloadStaffAttendances={reloadStaffAttendances}
              openDetails={
                featureFlags.experimental?.staffAttendanceTypes
                  ? setDetailsModal
                  : undefined
              }
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
              deleteAttendances={deleteAttendances}
              enableNewEntries={enableNewEntries}
              reloadStaffAttendances={reloadStaffAttendances}
            />
          ))}
        </Tbody>
      </Table>
      {detailsModal && (
        <StaffAttendanceDetailsModal
          unitId={unitId}
          employeeId={detailsModal.employeeId}
          date={detailsModal.date}
          attendances={staffAttendances}
          close={closeModal}
          reloadStaffAttendances={reloadStaffAttendances}
        />
      )}
    </>
  )
})

type TimeRangeWithErrorsAndMetadata = TimeRangeWithErrors & {
  id?: UUID
  groupId?: UUID
  type?: StaffAttendanceType
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
const overnightTimeRangeAt = (
  date: LocalDate,
  groupId: UUID
): TimeRangeWithErrorsAndMetadata => ({
  startTime: '00:00',
  endTime: '23:59',
  errors: {
    startTime: undefined,
    endTime: undefined
  },
  lastSavedState: {
    startTime: '',
    endTime: ''
  },
  id: undefined,
  groupId,
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
  deleteAttendances: (
    staffAttendanceIds: UUID[],
    externalStaffAttendanceIds: UUID[]
  ) => Promise<Result<void>[]>
  enableNewEntries?: boolean
  reloadStaffAttendances: () => void
  openDetails?: (v: { employeeId: string; date: LocalDate }) => void
}

const AttendanceRow = React.memo(function AttendanceRow({
  rowIndex,
  isPositiveOccupancyCoefficient,
  name,
  employeeId,
  operationalDays,
  attendances,
  saveAttendances,
  deleteAttendances,
  enableNewEntries,
  reloadStaffAttendances,
  openDetails
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
        !attendance.arrived
          .toLocalDate()
          .isEqual(attendance.departed.toLocalDate())
      ) {
        return [
          {
            ...attendance,
            departed: attendance.arrived.withTime(LocalTime.of(23, 59))
          },
          {
            ...attendance,
            id: '',
            arrived: attendance.departed.withTime(LocalTime.of(0, 0))
          }
        ]
      }
      return attendance
    })

    const daysAndRanges = operationalDays.map((day) => {
      const date = day.date

      const attendancesOnDay = dailyAttendances.filter((a) =>
        a.arrived.toLocalDate().isEqual(date)
      )

      const laterAttendances = sortBy(
        dailyAttendances.filter((a) =>
          a.arrived.isAfter(HelsinkiDateTime.fromLocal(date, LocalTime.MIN))
        ),
        (a) => a.arrived.timestamp
      )
      const nextEntry =
        laterAttendances.length > 0 ? laterAttendances[0] : undefined

      const nextEntryIsOvernight =
        nextEntry &&
        nextEntry.arrived.hour == 0 &&
        nextEntry.arrived.minute == 0

      return {
        date: day.date,
        timeRanges:
          attendancesOnDay.length > 0
            ? attendancesOnDay.map((attendance) => {
                const startTime = attendance.arrived
                  .toLocalTime()
                  .format('HH:mm')
                const endTime =
                  attendance.departed?.toLocalTime().format('HH:mm') ?? ''
                return {
                  id: attendance.id,
                  groupId: attendance.groupId,
                  type: attendance.type,
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
            : nextEntryIsOvernight
            ? [overnightTimeRangeAt(day.date, nextEntry.groupId)]
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

  const saveChanges = useCallback(async () => {
    setEditing(false)

    const allTimeRanges = values.flatMap(({ timeRanges }) => timeRanges)

    const rangesToSave = allTimeRanges
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

    const rangesToDelete = allTimeRanges.filter(
      (r) =>
        r.id &&
        r.startTime === '' &&
        r.endTime === '' &&
        !rangesToSave.includes(r)
    )

    if (rangesToDelete.length > 0) {
      // rangesToDelete includes only entries where id is defined
      await deleteAttendances(
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        employeeId ? rangesToDelete.map((r) => r.id!) : [],
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        !employeeId ? rangesToDelete.map((r) => r.id!) : []
      )
    }

    const attendancesToSave = rangesToSave.map((range) => ({
      attendanceId: !range.id || range.id === '' ? null : range.id,
      arrived: HelsinkiDateTime.fromLocal(
        range.arrived,
        LocalTime.parse(range.startTime, 'HH:mm')
      ),
      departed:
        range.endTime !== ''
          ? HelsinkiDateTime.fromLocal(
              range.departed,
              LocalTime.parse(range.endTime, 'HH:mm')
            )
          : null,
      groupId: range.groupId || '',
      type: range.type ?? 'PRESENT'
    }))

    return saveAttendances({
      staffAttendances: employeeId
        ? attendancesToSave.map((r) => ({ ...r, employeeId }))
        : [],
      externalAttendances: !employeeId
        ? attendancesToSave.map((r) => ({ ...r, name }))
        : []
    }).then(() => reloadStaffAttendances())
  }, [
    setEditing,
    values,
    saveAttendances,
    deleteAttendances,
    reloadStaffAttendances,
    employeeId,
    name
  ])

  const renderArrivalTime = useCallback((time: string) => {
    switch (time) {
      case '':
        return '-'
      case '00:00':
        return '→'
      default:
        return time
    }
  }, [])
  const renderDepartureTime = useCallback((time: string) => {
    switch (time) {
      case '':
        return '-'
      case '23:59':
        return '→'
      default:
        return time
    }
  }, [])

  return (
    <DayTr data-qa={`attendance-row-${rowIndex}`}>
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
      {values.map(({ date, timeRanges }) => {
        const [shownTimeRanges, hiddenTimeRanges] = partition(
          timeRanges,
          ({ type }) => type === undefined || type === 'PRESENT'
        )
        return (
          <DayTd
            key={date.formatIso()}
            className={classNames({ 'is-today': date.isToday() })}
            partialRow={false}
            rowIndex={rowIndex}
            data-qa={`day-cell-${employeeId ?? ''}-${date.formatIso()}`}
          >
            <DayCell>
              <AttendanceTimes>
                {shownTimeRanges.map((range, rangeIx) => (
                  <AttendanceCell key={`${date.formatIso()}-${rangeIx}`}>
                    {editing && (range.groupId || enableNewEntries) ? (
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
                        <AttendanceTime data-qa="arrival-time">
                          {renderArrivalTime(range.startTime)}
                        </AttendanceTime>
                        <AttendanceTime data-qa="departure-time">
                          {renderDepartureTime(range.endTime)}
                        </AttendanceTime>
                      </>
                    )}
                  </AttendanceCell>
                ))}
              </AttendanceTimes>
              {!!employeeId && openDetails && !editing && (
                <DetailsToggle showAlways={hiddenTimeRanges.length > 0}>
                  <IconButton
                    icon={faCircleEllipsis}
                    onClick={() => openDetails({ employeeId, date })}
                    data-qa={`open-details-${employeeId}-${date.formatIso()}`}
                  />
                </DetailsToggle>
              )}
            </DayCell>
          </DayTd>
        )
      })}
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

const DetailsToggle = styled.div<{ showAlways: boolean }>`
  display: flex;
  align-items: center;
  padding: ${defaultMargins.xxs};
  margin-left: -${defaultMargins.s};
  visibility: ${({ showAlways }) => (showAlways ? 'visible' : 'hidden')};
`

const DayCell = styled.div`
  display: flex;

  &:hover {
    ${DetailsToggle} {
      visibility: visible;
    }
  }
`

const AttendanceTimes = styled.div`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`

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
      data-qa="row-menu"
    />
  )
})
