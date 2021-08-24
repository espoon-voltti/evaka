// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { useTranslation } from 'employee-mobile-frontend/state/i18n'
import { AttendanceChild } from 'employee-mobile-frontend/api/attendances'
import { ArrivalTime } from '../components'
import { getTimeString } from './ChildInfo'

interface Props {
  child: AttendanceChild
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
              ? getTimeString(child.attendance.arrived)
              : 'xx:xx'}
          </span>
        </ArrivalTime>
      ) : null}
      {showDeparture ? (
        <ArrivalTime>
          <span>{i18n.attendances.departureTime}</span>
          <span>
            {child.attendance?.departed
              ? getTimeString(child.attendance.departed)
              : 'xx:xx'}
          </span>
        </ArrivalTime>
      ) : null}
    </FixedSpaceRow>
  )
})
