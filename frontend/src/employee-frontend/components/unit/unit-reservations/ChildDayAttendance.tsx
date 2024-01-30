// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCircleEllipsis } from 'Icons'
import React, { useCallback, useMemo } from 'react'
import styled from 'styled-components'

import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/daily-service-times'
import { ChildServiceNeedInfo } from 'lib-common/generated/api-types/absence'
import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import { ScheduleType } from 'lib-common/generated/api-types/placement'
import {
  OpenTimeRange,
  Reservation,
  UnitDateInfo
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { reservationHasTimes, TimeRange } from 'lib-common/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { fontWeights } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

import { DateCell, DetailsToggle, TimeCell, TimesRow } from './ChildDayCommons'

interface Props {
  date: LocalDate
  attendanceIndex: number
  dateInfo: UnitDateInfo
  attendance: OpenTimeRange | undefined
  reservations: Reservation[]
  dailyServiceTimes: DailyServiceTimesValue | null
  inOtherUnit: boolean
  isInBackupGroup: boolean
  scheduleType: ScheduleType
  serviceNeedInfo: ChildServiceNeedInfo | undefined
  onStartEdit: () => void
}

export default React.memo(function ChildDayAttendance({
  date,
  attendanceIndex,
  dateInfo,
  attendance,
  reservations,
  dailyServiceTimes,
  inOtherUnit,
  isInBackupGroup,
  scheduleType,
  serviceNeedInfo,
  onStartEdit
}: Props) {
  const { i18n } = useTranslation()

  const expectedTimes = useMemo(() => {
    const serviceTimeRange =
      dailyServiceTimes === null || isVariableTime(dailyServiceTimes)
        ? null
        : isRegular(dailyServiceTimes)
          ? dailyServiceTimes.regularTimes
          : isIrregular(dailyServiceTimes)
            ? getTimesOnWeekday(dailyServiceTimes, date.getIsoDayOfWeek())
            : null

    return getExpectedAttendanceTimes(
      reservations,
      serviceTimeRange
        ? {
            startTime: serviceTimeRange.start,
            endTime: serviceTimeRange.end
          }
        : null
    )
  }, [dailyServiceTimes, reservations, date])

  const isWithinExpectedTimes = useCallback(
    (time: LocalTime) =>
      expectedTimes.some(
        (expected) =>
          expected.startTime.isEqualOrBefore(time) &&
          expected.endTime.isEqualOrAfter(time)
      ),
    [expectedTimes]
  )

  const intermittent = serviceNeedInfo?.shiftCare === 'INTERMITTENT'
  if (dateInfo.isHoliday && !attendance && !intermittent) return null

  const unitIsNotOpenOnReservationStart = reservations.some(
    (reservation) =>
      dateInfo.time === null ||
      dateInfo.isHoliday ||
      (reservation.type === 'TIMES' &&
        dateInfo.time.start > reservation.startTime)
  )

  const unitIsNotOpenOnReservationEnd = reservations.some(
    (reservation) =>
      dateInfo.time === null ||
      dateInfo.isHoliday ||
      (reservation.type === 'TIMES' && dateInfo.time.end < reservation.endTime)
  )

  const requiresBackupCare =
    attendanceIndex === 0 &&
    attendance === undefined &&
    intermittent &&
    (unitIsNotOpenOnReservationStart || unitIsNotOpenOnReservationEnd) &&
    !inOtherUnit

  return (
    <AttendanceDateCell>
      {!inOtherUnit && !isInBackupGroup ? (
        <TimesRow data-qa={`attendance-${date.formatIso()}-${attendanceIndex}`}>
          {requiresBackupCare ? (
            <TimeCell data-qa="backup-care-required-warning" warning>
              {i18n.unit.attendanceReservations.requiresBackupCare}{' '}
              <FontAwesomeIcon
                icon={faExclamationTriangle}
                color={colors.status.warning}
              />
            </TimeCell>
          ) : (
            <>
              <AttendanceTime
                data-qa="attendance-start"
                warning={
                  attendance
                    ? !isWithinExpectedTimes(attendance.startTime)
                    : false
                }
              >
                {attendance?.startTime?.format() ?? '-'}
              </AttendanceTime>
              <AttendanceTime
                data-qa="attendance-end"
                warning={
                  attendance?.endTime
                    ? !isWithinExpectedTimes(attendance.endTime)
                    : false
                }
              >
                {attendance?.endTime?.format() ?? '-'}
              </AttendanceTime>
            </>
          )}
        </TimesRow>
      ) : null}
      {!inOtherUnit && !isInBackupGroup && scheduleType !== 'TERM_BREAK' && (
        <DetailsToggle>
          <IconButton
            icon={faCircleEllipsis}
            onClick={onStartEdit}
            data-qa="open-details"
            aria-label={i18n.common.open}
          />
        </DetailsToggle>
      )}
    </AttendanceDateCell>
  )
})

const getExpectedAttendanceTimes = (
  reservations: Reservation[],
  serviceTimeOfDay: TimeRange | null
): TimeRange[] => {
  const reservationTimes = reservations
    .filter(reservationHasTimes)
    .map((r) => ({
      startTime: r.startTime,
      endTime: r.endTime
    }))

  if (reservationTimes.length > 0) return reservationTimes

  if (serviceTimeOfDay) return [serviceTimeOfDay]

  return []
}

export const AttendanceDateCell = styled(DateCell)`
  background-color: ${colors.grayscale.g4};
  position: relative;
  height: 38px;
  padding-right: 12px;

  &:hover {
    ${DetailsToggle} {
      visibility: visible;
    }
  }
`

const AttendanceTime = styled(TimeCell)`
  font-weight: ${fontWeights.semibold};
`
