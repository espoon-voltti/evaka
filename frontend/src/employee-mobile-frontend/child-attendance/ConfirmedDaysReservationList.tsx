// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import {
  ReservationChildInfo,
  UnitDailyReservationInfo
} from 'lib-common/generated/api-types/reservations'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'

import { useSelectedGroup } from '../common/selected-group'

import DayList from './DayList'

interface Props {
  dailyReservations: UnitDailyReservationInfo[]
  childMap: Record<UUID, ReservationChildInfo>
}

export default React.memo(function ConfirmedDaysReservationList({
  dailyReservations,
  childMap
}: Props) {
  const { selectedGroupId } = useSelectedGroup()

  const groupReservations = useMemo(
    () =>
      selectedGroupId.type === 'all'
        ? dailyReservations
        : dailyReservations.map((day) => ({
            ...day,
            reservationInfos: day.reservationInfos.filter(
              (i) => i.groupId === selectedGroupId.id
            )
          })),
    [selectedGroupId, dailyReservations]
  )

  return (
    <>
      <ContentArea
        opaque={false}
        paddingVertical="zero"
        paddingHorizontal="zero"
      >
        <DayList reservationDays={groupReservations} childMap={childMap} />
      </ContentArea>
    </>
  )
})
