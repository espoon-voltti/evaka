// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '../../state/i18n'
import {
  DailyServiceTimes,
  isIrregular,
  isRegular,
  TimeRange
} from 'lib-common/api-types/child/common'
import { ServiceTime } from './components'

const dayNames = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday'
] as const

type DayName = typeof dayNames[number]

function getToday(): DayName | undefined {
  // Sunday is 0
  const dayIndex = (new Date().getDay() + 6) % 7
  return dayNames[dayIndex]
}

function getTodaysServiceTimes(
  times: DailyServiceTimes | null
): TimeRange | 'not_today' | 'not_set' {
  if (times === null) return 'not_set'

  if (isRegular(times)) return times.regularTimes

  if (isIrregular(times)) {
    const today = getToday()
    if (!today) return 'not_today'

    return times[today] ?? 'not_today'
  }

  return 'not_set'
}

interface Props {
  times: DailyServiceTimes | null
}

export default React.memo(function AttendanceDailyServiceTimes({
  times
}: Props) {
  const { i18n } = useTranslation()

  const todaysTimes = getTodaysServiceTimes(times)
  return (
    <ServiceTime>
      {todaysTimes === 'not_set' ? (
        <em>{i18n.attendances.serviceTime.notSet}</em>
      ) : todaysTimes === 'not_today' ? (
        i18n.attendances.serviceTime.noServiceToday
      ) : (
        i18n.attendances.serviceTime.serviceToday(
          todaysTimes.start,
          todaysTimes.end
        )
      )}
    </ServiceTime>
  )
})
