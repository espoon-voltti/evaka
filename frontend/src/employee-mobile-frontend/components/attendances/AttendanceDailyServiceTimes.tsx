// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '../../state/i18n'
import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import { AttendanceReservation } from '../../api/attendances'
import { getTodaysServiceTimes } from '../../utils/dailyServiceTimes'
import { ServiceTime } from './components'

interface Props {
  times: DailyServiceTimes | null
  reservation: AttendanceReservation | null
}

export default React.memo(function AttendanceDailyServiceTimes({
  times,
  reservation
}: Props) {
  const { i18n } = useTranslation()

  const todaysTimes = getTodaysServiceTimes(times)
  return (
    <ServiceTime>
      {reservation !== null ? (
        i18n.attendances.serviceTime.reservation(
          reservation.startTime,
          reservation.endTime
        )
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
