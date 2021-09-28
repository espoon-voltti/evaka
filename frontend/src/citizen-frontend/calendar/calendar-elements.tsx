// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Translations, useTranslation } from 'citizen-frontend/localization'
import { ChildDailyData, DailyReservationData } from './api'

export const Reservations = React.memo(function Reservations({
  data
}: {
  data: DailyReservationData
}) {
  const i18n = useTranslation()
  const reservations = uniqueReservations(i18n, data.children)

  return reservations.length === 0 ? (
    data.isHoliday ? (
      <Holiday />
    ) : (
      <NoReservation />
    )
  ) : (
    <span>{reservations.join(', ')}</span>
  )
})

export const Holiday = React.memo(function Holiday() {
  const i18n = useTranslation()
  return <HolidayNote>{i18n.calendar.holiday}</HolidayNote>
})

const HolidayNote = styled.span`
  font-style: italic;
  color: ${({ theme }) => theme.colors.greyscale.dark};
`

export const NoReservation = React.memo(function NoReservation() {
  const i18n = useTranslation()
  return <NoReservationNote>{i18n.calendar.noReservation}</NoReservationNote>
})

const NoReservationNote = styled.span`
  color: ${({ theme }) => theme.colors.accents.orangeDark};
`

const uniqueReservations = (
  i18n: Translations,
  reservations: ChildDailyData[]
): string[] => {
  const uniqueReservationTimes: string[] = reservations
    .map(({ absence, reservation }) =>
      absence === null && reservation !== null
        ? `${reservation.startTime} – ${reservation.endTime}`
        : undefined
    )
    .filter((reservation): reservation is string => reservation !== undefined)
    .reduce<string[]>(
      (uniq, reservation) =>
        uniq.some((res) => res === reservation) ? uniq : [...uniq, reservation],
      []
    )

  const someoneIsAbsent = reservations.some(({ absence }) => absence !== null)

  return [
    ...(someoneIsAbsent ? [i18n.calendar.absent] : []),
    ...uniqueReservationTimes
  ]
}
