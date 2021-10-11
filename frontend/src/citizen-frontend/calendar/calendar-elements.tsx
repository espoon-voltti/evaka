// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import {
  ChildDailyData,
  DailyReservationData
} from 'lib-common/generated/api-types/reservations'
import { Translations, useTranslation } from '../localization'

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
  data: ChildDailyData[]
): string[] => {
  const uniqueReservationTimes: string[] = data
    .flatMap(({ absence, reservations }) =>
      absence === null
        ? reservations.map(
            ({ startTime, endTime }) => `${startTime} – ${endTime}`
          )
        : []
    )
    .filter((reservation): reservation is string => reservation !== undefined)
    .reduce<string[]>(
      (uniq, reservation) =>
        uniq.some((res) => res === reservation) ? uniq : [...uniq, reservation],
      []
    )
    .sort()

  const someoneIsAbsent = data.some(({ absence }) => absence !== null)

  return [
    ...(someoneIsAbsent ? [i18n.calendar.absent] : []),
    ...uniqueReservationTimes
  ]
}
