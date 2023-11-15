// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'

import { Reservation } from 'lib-common/generated/api-types/reservations'

import { useTranslation } from '../common/i18n'

interface Props {
  hideLabel?: boolean
  reservations: Reservation.Times[]
}

export const Reservations = React.memo(function Reservations({
  hideLabel,
  reservations
}: Props) {
  const { i18n } = useTranslation()

  if (reservations.length === 0) {
    return null
  }

  const label =
    reservations.length === 1
      ? i18n.attendances.serviceTime.reservation
      : i18n.attendances.serviceTime.reservations

  return (
    <>
      {!hideLabel && <span>{label}:</span>}
      <Whitespace />
      {reservations.map(({ startTime, endTime }, index) => (
        <Fragment key={startTime.formatIso()}>
          {index !== 0 && <Separator />}
          <span>{startTime.format()}</span>
          <Dash />
          <span>{endTime.format()}</span>
        </Fragment>
      ))}
    </>
  )
})

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

const Whitespace = styled.span`
  ::before {
    content: ' ';
  }
`
