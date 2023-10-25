// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/daily-service-times'
import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import { ChildServiceNeedInfo } from 'lib-common/generated/api-types/daycare'
import { ScheduleType } from 'lib-common/generated/api-types/placement'
import {
  Absence,
  Reservation,
  UnitDateInfo
} from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { TimeRange } from 'lib-common/reservations'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Light } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

import AbsenceDay from './AbsenceDay'
import { DateCell, TimeCell, TimesRow } from './ChildDay'

interface Props {
  date: LocalDate
  reservationIndex: number
  editing: boolean
  dateInfo: UnitDateInfo
  reservation: Reservation | undefined
  absence: Absence | null
  dailyServiceTimes: DailyServiceTimesValue | null
  inOtherUnit: boolean
  isInBackupGroup: boolean
  scheduleType: ScheduleType
  serviceNeedInfo: ChildServiceNeedInfo | undefined
  deleteAbsence: () => void
  updateReservation: (r: JsonOf<TimeRange>) => void
  saveReservation: (d: LocalDate) => void
}

export default React.memo(function ChildDay({
  date,
  reservationIndex,
  editing,
  dateInfo,
  reservation,
  absence,
  dailyServiceTimes,
  inOtherUnit,
  isInBackupGroup,
  scheduleType,
  serviceNeedInfo,
  deleteAbsence
}: Props) {
  const { i18n } = useTranslation()

  const intermittent = serviceNeedInfo?.shiftCare === 'INTERMITTENT'
  if (dateInfo.isHoliday && !reservation && !intermittent) return null

  const serviceTimeOfDay =
    dailyServiceTimes === null || isVariableTime(dailyServiceTimes)
      ? null
      : isRegular(dailyServiceTimes)
      ? dailyServiceTimes.regularTimes
      : isIrregular(dailyServiceTimes)
      ? getTimesOnWeekday(dailyServiceTimes, date.getIsoDayOfWeek())
      : null

  const unitIsNotOpenOnReservationStart =
    reservation !== undefined &&
    (dateInfo.time === null ||
      dateInfo.isHoliday ||
      (reservation.type === 'TIMES' &&
        dateInfo.time.start > reservation.startTime))

  const unitIsNotOpenOnReservationEnd =
    reservation !== undefined &&
    (dateInfo.time === null ||
      dateInfo.isHoliday ||
      (reservation.type === 'TIMES' && dateInfo.time.end < reservation.endTime))

  return (
    <DateCell>
      <TimesRow data-qa={`reservation-${date.formatIso()}-${reservationIndex}`}>
        {inOtherUnit ? (
          <TimeCell data-qa="in-other-unit">
            <Light>{i18n.unit.attendanceReservations.inOtherUnit}</Light>
          </TimeCell>
        ) : isInBackupGroup ? (
          <TimeCell data-qa="in-other-group">
            <Light>{i18n.unit.attendanceReservations.inOtherGroup}</Light>
          </TimeCell>
        ) : absence ? (
          <AbsenceDay
            type={absence.type}
            onDelete={editing ? () => deleteAbsence() : undefined}
          />
        ) : editing ? (
          <div>todo</div>
        ) : /*<TimeRangeEditor
            timeRange={editState.reservations[rowIndex][day.date.formatIso()]}
            update={(timeRange) =>
              updateReservation(rowIndex, day.date, timeRange)
            }
            save={() => saveReservation(day.date)}
          />*/
        reservation && reservation.type === 'TIMES' ? (
          // a reservation exists for this day
          <>
            <ReservationTime
              data-qa="reservation-start"
              warning={unitIsNotOpenOnReservationStart}
            >
              {reservation.startTime.format()}
              {unitIsNotOpenOnReservationStart && (
                <>
                  {' '}
                  <FontAwesomeIcon
                    icon={faExclamationTriangle}
                    color={colors.status.warning}
                  />
                </>
              )}
            </ReservationTime>
            <ReservationTime
              data-qa="reservation-end"
              warning={unitIsNotOpenOnReservationEnd}
            >
              {reservation.endTime.format()}
              {unitIsNotOpenOnReservationEnd && (
                <>
                  {' '}
                  <FontAwesomeIcon
                    icon={faExclamationTriangle}
                    color={colors.status.warning}
                  />
                </>
              )}
            </ReservationTime>
          </>
        ) : reservationIndex === 0 ? (
          dateInfo.isInHolidayPeriod &&
          scheduleType === 'RESERVATION_REQUIRED' &&
          reservation === null &&
          reservationIndex === 0 ? (
            // holiday period, no reservation yet
            <Tooltip
              tooltip={
                i18n.unit.attendanceReservations.missingHolidayReservation
              }
              position="top"
            >
              <ReservationTime warning data-qa="holiday-reservation-missing">
                {
                  i18n.unit.attendanceReservations
                    .missingHolidayReservationShort
                }
              </ReservationTime>
            </Tooltip>
          ) : scheduleType === 'TERM_BREAK' ? (
            <ReservationTime data-qa="term-break">
              {i18n.unit.attendanceReservations.termBreak}
            </ReservationTime>
          ) : serviceTimeOfDay ? (
            // daily service times
            <>
              <ReservationTime data-qa="reservation-start">
                {serviceTimeOfDay.start.format()}{' '}
                {i18n.unit.attendanceReservations.serviceTimeIndicator}
              </ReservationTime>
              <ReservationTime data-qa="reservation-end">
                {serviceTimeOfDay.end.format()}{' '}
                {i18n.unit.attendanceReservations.serviceTimeIndicator}
              </ReservationTime>
            </>
          ) : scheduleType === 'FIXED_SCHEDULE' ? (
            <ReservationTime data-qa="fixed-schedule">
              {i18n.unit.attendanceReservations.fixedSchedule}
            </ReservationTime>
          ) : (
            // otherwise show missing service time indicator
            <ReservationTime warning data-qa="reservation-missing">
              {i18n.unit.attendanceReservations.missingServiceTime}
            </ReservationTime>
          )
        ) : null}
      </TimesRow>
    </DateCell>
  )
})

const ReservationTime = styled(TimeCell)``
