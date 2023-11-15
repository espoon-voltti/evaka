// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import { Reservation } from 'lib-common/generated/api-types/reservations'

import { Reservations } from '../child-attendance/Reservations'
import { ServiceTime } from '../common/components'
import { getTodaysServiceTimes } from '../common/dailyServiceTimes'
import { useTranslation } from '../common/i18n'

interface Props {
  hideLabel?: boolean
  times: DailyServiceTimesValue | null
  reservations: Reservation[]
}

export default React.memo(function AttendanceDailyServiceTimes({
  hideLabel,
  times,
  reservations
}: Props) {
  const { i18n } = useTranslation()
  const reservationsWithTimes = useMemo(
    () =>
      reservations.flatMap((reservation) =>
        reservation.type === 'TIMES' ? [reservation] : []
      ),
    [reservations]
  )

  const todaysTimes = getTodaysServiceTimes(times)
  return (
    <ServiceTime>
      {reservationsWithTimes.length > 0 ? (
        <Reservations
          hideLabel={hideLabel}
          reservations={reservationsWithTimes}
        />
      ) : todaysTimes === 'not_set' ? (
        <em>{i18n.attendances.serviceTime.notSet}</em>
      ) : todaysTimes === 'not_today' ? (
        i18n.attendances.serviceTime.noServiceToday
      ) : todaysTimes === 'variable_times' ? (
        i18n.attendances.serviceTime.variableTimes
      ) : (
        i18n.attendances.serviceTime.serviceToday(
          todaysTimes.start.format(),
          todaysTimes.end.format()
        )
      )}
    </ServiceTime>
  )
})
