// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  ReservationResponse,
  TimeInterval,
  UsedServiceResult
} from 'lib-common/generated/api-types/reservations'
import { reservationsAndAttendancesDiffer } from 'lib-common/reservations'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'

import { useTranslation } from '../localization'

import { ReservationAttendanceHeading } from './DayView'
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
      <div>
        {attendances.length > 0
          ? attendances.map(({ startTime, endTime }) => {
              const start = startTime.format()
              const end = endTime?.format() ?? ''
              return (
                <div key={`${start}-${end}`}>
                  {start} – {end}
                </div>
              )
            })
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
    attendances[0].startTime < reservations[0].startTime
  const simpleExceedEnd =
    attendances.length === 1 &&
    reservations.length === 1 &&
    reservations[0].type === 'TIMES' &&
    !!attendances[0].endTime &&
    attendances[0].endTime > reservations[0].endTime
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
      <div>
        {attendances.length > 0 ? (
          <>
            {attendances.map(({ startTime, endTime }) => {
              const start = startTime.format()
              const end = endTime?.format() ?? ''
              return (
                <div key={`${start}-${end}`}>
                  <TimeDisplay highlight={simpleExceedStart}>
                    {start}
                  </TimeDisplay>
                  –<TimeDisplay highlight={simpleExceedEnd}>{end}</TimeDisplay>
                </div>
              )
            })}
            {extraUsageMinutes > 0 && (
              <>
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
        )}{' '}
        {showAttendanceWarning && <AlertBox message={attendanceWarning} wide />}
      </div>
      <ReservationAttendanceHeading>
        {i18n.calendar.usedService}
      </ReservationAttendanceHeading>
      <div>
        {usedService.usedServiceRanges.length > 0 ||
        usedService.usedServiceMinutes > 0 ? (
          <>
            {usedService.usedServiceRanges.map((timeRange, index) => (
              <div key={index}>{timeRange.format()}</div>
            ))}
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
