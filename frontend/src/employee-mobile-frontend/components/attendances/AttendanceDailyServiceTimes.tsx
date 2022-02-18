// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import { AttendanceReservation } from 'lib-common/generated/api-types/attendance'

import { useTranslation } from '../../state/i18n'
import { getTodaysServiceTimes } from '../../utils/dailyServiceTimes'

import { Reservations } from './Reservations'
import { ServiceTime } from './components'

interface Props {
  times: DailyServiceTimes | null
  reservations: AttendanceReservation[]
}

export default React.memo(function AttendanceDailyServiceTimes({
  times,
  reservations
}: Props) {
  const { i18n } = useTranslation()

  const todaysTimes = getTodaysServiceTimes(times)
  return (
    <ServiceTime>
      {reservations.length > 0 ? (
        <Reservations i18n={i18n} reservations={reservations} />
      ) : todaysTimes === 'not_set' ? (
        <em>{i18n.attendances.serviceTime.notSet}</em>
      ) : todaysTimes === 'not_today' ? (
        i18n.attendances.serviceTime.noServiceToday
      ) : todaysTimes === 'variable_times' ? (
        i18n.attendances.serviceTime.variableTimes
      ) : (
        i18n.attendances.serviceTime.serviceToday(
          todaysTimes.start,
          todaysTimes.end
        )
      )}
    </ServiceTime>
  )
})
