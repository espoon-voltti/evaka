// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'

import { AttendanceReservation } from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import { Translations } from '../../state/i18n'

interface Props {
  i18n: Translations
  reservations: AttendanceReservation[]
}

export const Reservations = React.memo(function Reservations({
  i18n,
  reservations
}: Props) {
  if (reservations.length === 0) {
    return null
  }

  const label =
    reservations.length === 1
      ? i18n.attendances.serviceTime.reservation
      : i18n.attendances.serviceTime.reservations

  return (
    <>
      <span>{label}:</span>
      <Whitespace />
      {reservations.map(({ startTime, endTime }, index) => {
        const startDatePart = datePart(startTime, i18n)
        const endDatePart = datePart(endTime, i18n)

        return (
          <Fragment key={startTime.formatIso()}>
            {index !== 0 && <Separator />}
            <span>{startTime.toLocalTime().format('HH:mm')}</span>
            {!!startDatePart && (
              <>
                <Whitespace />
                <DatePart>{startDatePart}</DatePart>
              </>
            )}
            <Dash />
            <span>{endTime.toLocalTime().format('HH:mm')}</span>
            {!!endDatePart && (
              <>
                <Whitespace />
                <DatePart>{endDatePart}</DatePart>
              </>
            )}
          </Fragment>
        )
      })}
    </>
  )
})

function datePart(
  dateTime: HelsinkiDateTime,
  i18n: Translations
): string | undefined {
  const localDate = dateTime.toLocalDate()
  if (localDate.isToday()) {
    return undefined
  }

  const datePart = localDate.isEqual(LocalDate.todayInSystemTz().addDays(1))
    ? i18n.attendances.serviceTime.tomorrow
    : localDate.isEqual(LocalDate.todayInSystemTz().subDays(1))
    ? i18n.attendances.serviceTime.yesterday
    : localDate.format('d.M.')

  return `(${datePart})`
}

const Dash = React.memo(function Dash() {
  return (
    <>
      <Whitespace />–<Whitespace />
    </>
  )
})

const Separator = React.memo(function Separator() {
  return (
    <>
      ,<Whitespace />
    </>
  )
})

const DatePart = styled.span`
  color: ${({ theme }) => theme.colors.grayscale.g35};
`

const Whitespace = styled.span`
  ::before {
    content: ' ';
  }
`
