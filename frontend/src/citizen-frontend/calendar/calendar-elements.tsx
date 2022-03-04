// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import {
  ChildDailyData,
  DailyReservationData
} from 'lib-common/generated/api-types/reservations'
import { reservationsAndAttendancesDiffer } from 'lib-common/reservations'
import StatusIcon from 'lib-components/atoms/StatusIcon'
import { Light } from 'lib-components/typography'

import { Translations, useTranslation } from '../localization'

export const Reservations = React.memo(function Reservations({
  data
}: {
  data: DailyReservationData
}) {
  const i18n = useTranslation()
  const reservations = uniqueReservations(i18n, data.children)
  const showAttendanceWarning = data.children.some(
    ({ reservations, attendances }) =>
      reservationsAndAttendancesDiffer(reservations, attendances)
  )

  return reservations.length === 0 ? (
    data.isHoliday ? (
      <Holiday />
    ) : (
      <NoReservation />
    )
  ) : (
    <span>
      {reservations.join(', ')}
      {showAttendanceWarning && <StatusIcon status="warning" />}
    </span>
  )
})

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <Light>{i18n.calendar.holiday}</Light>
})

export const NoReservation = React.memo(function NoReservation() {
  const i18n = useTranslation()
  return <NoReservationNote>{i18n.calendar.noReservation}</NoReservationNote>
})

const NoReservationNote = styled.span`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

const uniqueReservations = (
  i18n: Translations,
  data: ChildDailyData[]
): string[] => {
  const reservationTimes = [
    ...new Set(
      data.flatMap(({ absence, reservations }) =>
        absence === null
          ? reservations.map((r) => `${r.startTime} – ${r.endTime}`)
          : []
      )
    )
  ].sort()

  const someoneIsAbsent = data.some(
    ({ absence }) => absence && absence !== 'FREE_ABSENCE'
  )
  const someoneIsOnFreeAbsence = data.some(
    ({ absence }) => absence === 'FREE_ABSENCE'
  )

  return [
    ...(someoneIsAbsent ? [i18n.calendar.absent] : []),
    ...(someoneIsOnFreeAbsence ? [i18n.calendar.absentFree] : []),
    ...reservationTimes
  ]
}
