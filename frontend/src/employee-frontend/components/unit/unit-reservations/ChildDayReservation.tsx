// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCircleEllipsis } from 'Icons'
import React from 'react'
import styled from 'styled-components'

import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/daily-service-times'
import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import {
  AbsenceType,
  ChildServiceNeedInfo
} from 'lib-common/generated/api-types/daycare'
import { ScheduleType } from 'lib-common/generated/api-types/placement'
import {
  ReservationResponse,
  UnitDateInfo
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import Tooltip from 'lib-components/atoms/Tooltip'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Light } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'
import { faExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

import AbsenceDay from './AbsenceDay'
import { DateCell, DetailsToggle, TimeCell, TimesRow } from './ChildDayCommons'

interface Props {
  date: LocalDate
  reservationIndex: number
  dateInfo: UnitDateInfo
  reservation: ReservationResponse | undefined
  absence: AbsenceType | null
  dailyServiceTimes: DailyServiceTimesValue | null
  inOtherUnit: boolean
  isInBackupGroup: boolean
  scheduleType: ScheduleType
  serviceNeedInfo: ChildServiceNeedInfo | undefined
  onStartEdit: () => void
}

export default React.memo(function ChildDayReservation({
  date,
  reservationIndex,
  dateInfo,
  reservation,
  absence,
  dailyServiceTimes,
  inOtherUnit,
  isInBackupGroup,
  scheduleType,
  serviceNeedInfo,
  onStartEdit
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
    <ReservationDateCell>
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
          reservationIndex === 0 ? (
            <AbsenceDay type={absence} />
          ) : null
        ) : reservation && reservation.type === 'TIMES' ? (
          <ReservationTooltip
            tooltip={
              reservation.staffCreated &&
              i18n.unit.attendanceReservations.createdByEmployee
            }
          >
            <TimeCell
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
                    data-qa="outside-opening-times"
                  />
                </>
              )}
            </TimeCell>
            <TimeCell
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
                    data-qa="outside-opening-times"
                  />
                </>
              )}
              {reservation.staffCreated && '*'}
            </TimeCell>
          </ReservationTooltip>
        ) : reservationIndex === 0 ? (
          dateInfo.isInHolidayPeriod &&
          scheduleType === 'RESERVATION_REQUIRED' &&
          reservation === undefined ? (
            // holiday period, no reservation yet
            <Tooltip
              tooltip={
                i18n.unit.attendanceReservations.missingHolidayReservation
              }
              position="top"
            >
              <TimeCell warning data-qa="holiday-reservation-missing">
                {
                  i18n.unit.attendanceReservations
                    .missingHolidayReservationShort
                }
              </TimeCell>
            </Tooltip>
          ) : scheduleType === 'TERM_BREAK' ? (
            <TimeCell data-qa="term-break">
              {i18n.unit.attendanceReservations.termBreak}
            </TimeCell>
          ) : serviceTimeOfDay ? (
            // daily service times
            <>
              <TimeCell data-qa="reservation-start">
                {serviceTimeOfDay.start.format()}{' '}
                {i18n.unit.attendanceReservations.serviceTimeIndicator}
              </TimeCell>
              <TimeCell data-qa="reservation-end">
                {serviceTimeOfDay.end.format()}{' '}
                {i18n.unit.attendanceReservations.serviceTimeIndicator}
              </TimeCell>
            </>
          ) : scheduleType === 'FIXED_SCHEDULE' ? (
            <TimeCell data-qa="fixed-schedule">
              {i18n.unit.attendanceReservations.fixedSchedule}
            </TimeCell>
          ) : reservation && reservation.type === 'NO_TIMES' ? (
            <TimeCell data-qa="reservation-no-times">
              {i18n.unit.attendanceReservations.reservationNoTimes}
            </TimeCell>
          ) : (
            // otherwise show missing reservation indicator
            <TimeCell warning data-qa="reservation-missing">
              {i18n.unit.attendanceReservations.missingReservation}
            </TimeCell>
          )
        ) : null}
      </TimesRow>
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
    </ReservationDateCell>
  )
})

export const ReservationDateCell = styled(DateCell)`
  position: relative;
  height: 38px;
  padding-right: 12px;

  &:hover {
    ${DetailsToggle} {
      visibility: visible;
    }
  }
`

const ReservationTooltip = styled(Tooltip)`
  display: flex;
  justify-content: space-evenly;
  padding: 8px;
  gap: 8px;
`
