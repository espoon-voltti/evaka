// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '../../state/i18n'
import { Translations } from '../../assets/i18n'
import { DailyServiceTimes, TimeRange } from '../../api/attendances'
import { ServiceTime } from './components'

const dayNames = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday'
] as const

type DayName = typeof dayNames[number]

function getToday(): DayName {
  return dayNames[new Date().getDay()]
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

  const timeRange = times.regular ? times.regularTimes : times[getToday()]
  if (timeRange === null) {
    return <ServiceTime>{noServiceToday(i18n)}</ServiceTime>
  }

  return <ServiceTime>{formatTimeRange(i18n, timeRange)}</ServiceTime>
})
