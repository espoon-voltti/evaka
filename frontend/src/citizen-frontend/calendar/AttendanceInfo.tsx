// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  ReservationResponse,
  UsedServiceResult
} from 'lib-common/generated/api-types/reservations'
import { reservationsAndAttendancesDiffer } from 'lib-common/reservations'
import TimeInterval from 'lib-common/time-interval'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'

import { useTranslation } from '../localization'

import {
  ReservationAttendanceSection,
  ReservationAttendanceHeading
} from './DayView'
import { HoursMinutes } from './MonthlyHoursSummary'

function AttendanceInfoWithSimpleThreshold({
  attendances,
  reservations
}: {
  attendances: TimeInterval[]
  reservations: ReservationResponse[]
}) {
  const i18n = useTranslation()
  const showAttendanceWarning = reservationsAndAttendancesDiffer(
    reservations,
    attendances
  )
  return (
    <>
      <ReservationAttendanceHeading>
        {i18n.calendar.attendance}
      </ReservationAttendanceHeading>
      <div data-qa="attendances">
        {attendances.length > 0
          ? attendances.map((timeInterval) => (
              <div key={timeInterval.format()}>{timeInterval.format()}</div>
            ))
          : '–'}

        {showAttendanceWarning && (
          <AlertBox message={i18n.calendar.attendanceWarning} wide />
        )}
      </div>
    </>
  )
}

const TimeDisplay = ({
  children,
  highlight
}: {
  children: React.ReactNode
  highlight: boolean
}) => (highlight ? <strong>{children}</strong> : <>{children}</>)

function AttendanceInfoWithServiceUsage({
  attendances,
  reservations,
  usedService
}: {
  attendances: TimeInterval[]
  reservations: ReservationResponse[]
  usedService: UsedServiceResult
}) {
  const i18n = useTranslation()
  const extraUsageMinutes =
    usedService.usedServiceMinutes - usedService.reservedMinutes

  const simpleExceedStart =
    attendances.length === 1 &&
    reservations.length === 1 &&
    reservations[0].type === 'TIMES' &&
    attendances[0].start.isBefore(reservations[0].range.start)
  const simpleExceedEnd =
    attendances.length === 1 &&
    reservations.length === 1 &&
    reservations[0].type === 'TIMES' &&
    !!attendances[0].end &&
    attendances[0].end.isAfter(reservations[0].range.end)
  const simpleExceedWarning =
    (simpleExceedStart ? i18n.calendar.exceedStart + ' ' : '') +
    (simpleExceedEnd ? i18n.calendar.exceedEnd : '')

  const showAttendanceWarning = extraUsageMinutes > 0
  const usageFromMonthlyAverage = usedService.usedServiceRanges.length === 0
  const attendanceWarning = usageFromMonthlyAverage
    ? i18n.calendar.calculatedUsedServiceTime
    : simpleExceedWarning != ''
      ? simpleExceedWarning
      : i18n.calendar.exceedGeneric
  return (
    <>
      <ReservationAttendanceHeading>
        {i18n.calendar.attendance}
      </ReservationAttendanceHeading>
      <div data-qa="attendances">
        {attendances.length > 0 ? (
          <>
            {attendances.map((timeInterval, index, array) => (
              <React.Fragment key={index}>
                <TimeDisplay highlight={simpleExceedStart}>
                  {timeInterval.formatStart()}
                </TimeDisplay>
                –
                <TimeDisplay highlight={simpleExceedEnd}>
                  {timeInterval.formatEnd()}
                </TimeDisplay>
                {index < array.length - 1 && ', '}
              </React.Fragment>
            ))}
            {extraUsageMinutes > 0 && (
              <>
                {' '}
                (
                <strong>
                  + <HoursMinutes minutes={extraUsageMinutes} />
                </strong>
                )
              </>
            )}
          </>
        ) : (
          '–'
        )}
      </div>
      {showAttendanceWarning && (
        <ReservationAttendanceSection data-qa="service-usage-warning">
          <AlertBox message={attendanceWarning} wide />
        </ReservationAttendanceSection>
      )}
      <ReservationAttendanceHeading>
        {i18n.calendar.usedService}
      </ReservationAttendanceHeading>
      <div data-qa="used-service">
        {usedService.usedServiceRanges.length > 0 ||
        usedService.usedServiceMinutes > 0 ? (
          <>
            {usedService.usedServiceRanges.map((timeRange, index, array) => (
              <React.Fragment key={index}>
                {timeRange.format()}
                {index < array.length - 1 && ', '}
              </React.Fragment>
            ))}{' '}
            (<HoursMinutes minutes={usedService.usedServiceMinutes} />)
          </>
        ) : (
          '–'
        )}
      </div>
    </>
  )
}

interface AttendanceInfoProps {
  attendances: TimeInterval[]
  reservations: ReservationResponse[]
  usedService: UsedServiceResult | null
}

export default React.memo(function AttendanceInfo({
  attendances,
  reservations,
  usedService
}: AttendanceInfoProps) {
  return usedService ? (
    <AttendanceInfoWithServiceUsage
      attendances={attendances}
      reservations={reservations}
      usedService={usedService}
    />
  ) : (
    <AttendanceInfoWithSimpleThreshold
      attendances={attendances}
      reservations={reservations}
    />
  )
})
