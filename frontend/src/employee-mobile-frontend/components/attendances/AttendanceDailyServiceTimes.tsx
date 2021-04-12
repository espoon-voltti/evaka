// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '../../state/i18n'
import { Translations } from '../../assets/i18n'
import {
  DailyServiceTimes,
  DailyServiceTimesIrregular,
  TimeRange
} from '../../api/attendances'
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
  // Sunday is 0. Adjust to dayNames indexing by subtracting 1.
  const dayIndex = new Date().getDay() - 1
  return dayNames[dayIndex]
}

function getTodaysServiceTimes(times: DailyServiceTimes): TimeRange | null {
  if (times.regular) return times.regularTimes

  const today = getToday()
  if (!today) return null

  return times[today] ?? null
}

function formatTimeRange(i18n: Translations, range: TimeRange) {
  return i18n.attendances.serviceTime.serviceToday(range.start, range.end)
}

function noServiceToday(i18n: Translations): string {
  return i18n.attendances.serviceTime.noServiceToday
}

interface Props {
  times: DailyServiceTimes | null
}

export default React.memo(function AttendanceDailyServiceTimes({
  times
}: Props) {
  const { i18n } = useTranslation()
  if (times === null) {
    return <ServiceTime>{noServiceToday(i18n)}</ServiceTime>
  }

  const timeRange = getTodaysServiceTimes(times)
  if (timeRange === null) {
    return <ServiceTime>{noServiceToday(i18n)}</ServiceTime>
  }

  return <ServiceTime>{formatTimeRange(i18n, timeRange)}</ServiceTime>
})
