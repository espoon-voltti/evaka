// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { formatTime } from 'lib-common/date'
import { Child } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { useTranslation } from '../../../state/i18n'
import { ArrivalTime } from '../components'

interface Props {
  child: Child
}

export default React.memo(function ArrivalAndDeparture({ child }: Props) {
  const { i18n } = useTranslation()

  const arrival = child.attendance?.arrived
  const departure = child.attendance?.departed

  if (!arrival) {
    return null
  }

  const arrivalDate = LocalDate.fromSystemTzDate(arrival)
  const dateInfo = arrivalDate.isEqual(LocalDate.today())
    ? ''
    : arrivalDate.isEqual(LocalDate.today().subDays(1))
    ? i18n.common.yesterday
    : arrivalDate.format('d.M.')

  return (
    <FixedSpaceRow justifyContent={'center'}>
      {arrival ? (
        <ArrivalTime>
          <span>{i18n.attendances.arrivalTime}</span>
          <span>{`${dateInfo} ${formatTime(arrival)}`}</span>
        </ArrivalTime>
      ) : null}
      {departure ? (
        <ArrivalTime>
          <span>{i18n.attendances.departureTime}</span>
          <span>{formatTime(departure)}</span>
        </ArrivalTime>
      ) : null}
    </FixedSpaceRow>
  )
})
