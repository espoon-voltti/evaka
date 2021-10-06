// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Child } from 'lib-common/generated/api-types/attendance'
import { useTranslation } from '../../../state/i18n'
import { formatTime } from 'lib-common/date'
import { ArrivalTime } from '../components'

interface Props {
  child: Child
}

export default React.memo(function ArrivalAndDeparture({ child }: Props) {
  const { i18n } = useTranslation()

  const showArrival = child.status === 'PRESENT' || child.status === 'DEPARTED'
  const showDeparture = child.status === 'DEPARTED'

  if (!showArrival && !showDeparture) {
    return null
  }

  return (
    <FixedSpaceRow justifyContent={'center'}>
      {showArrival ? (
        <ArrivalTime>
          <span>{i18n.attendances.arrivalTime}</span>
          <span>
            {child.attendance?.arrived
              ? formatTime(child.attendance.arrived)
              : 'xx:xx'}
          </span>
        </ArrivalTime>
      ) : null}
      {showDeparture ? (
        <ArrivalTime>
          <span>{i18n.attendances.departureTime}</span>
          <span>
            {child.attendance?.departed
              ? formatTime(child.attendance.departed)
              : 'xx:xx'}
          </span>
        </ArrivalTime>
      ) : null}
    </FixedSpaceRow>
  )
})
