// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import { ScheduleType } from 'lib-common/generated/api-types/placement'
import { Reservation } from 'lib-common/generated/api-types/reservations'

import { Reservations } from '../child-attendance/Reservations'
import { ServiceTime } from '../common/components'
import { getTodaysServiceTimes } from '../common/dailyServiceTimes'
import { useTranslation } from '../common/i18n'

interface Props {
  hideLabel?: boolean
  dailyServiceTimes: DailyServiceTimesValue | null
  reservations: Reservation[]
  scheduleType: ScheduleType
}

export default React.memo(function AttendanceDailyServiceTimes({
  hideLabel,
  dailyServiceTimes,
  reservations,
  scheduleType
}: Props) {
  const { i18n } = useTranslation()
  const reservationsWithTimes = useMemo(
    () =>
      reservations.flatMap((reservation) =>
        reservation.type === 'TIMES' ? [reservation] : []
      ),
    [reservations]
  )

  if (reservationsWithTimes.length > 0) {
    return (
      <ServiceTime data-qa="reservation">
        <Reservations
          hideLabel={hideLabel}
          reservations={reservationsWithTimes}
        />
      </ServiceTime>
    )
  }

  const todaysTimes = getTodaysServiceTimes(dailyServiceTimes)
  return (
    <ServiceTime data-qa="reservation">
      {scheduleType === 'FIXED_SCHEDULE' ? (
        i18n.attendances.serviceTime.present
      ) : scheduleType === 'TERM_BREAK' ? (
        // Should not happen because a term break child is absent
        i18n.attendances.termBreak
      ) : todaysTimes === 'not_set' ? (
        <em>{i18n.attendances.serviceTime.notSet}</em>
      ) : todaysTimes === 'not_today' ? (
        i18n.attendances.serviceTime.noServiceToday
      ) : todaysTimes === 'variable_times' ? (
        i18n.attendances.serviceTime.variableTimes
      ) : (
        i18n.attendances.serviceTime.serviceToday(
          todaysTimes.formatStart(),
          todaysTimes.formatEnd()
        )
      )}
    </ServiceTime>
  )
})
