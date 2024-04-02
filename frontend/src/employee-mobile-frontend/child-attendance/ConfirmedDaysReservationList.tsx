// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import { DayReservationStatisticsResult } from 'lib-common/generated/api-types/reservations'
import { ContentArea } from 'lib-components/layout/Container'

import { UnitOrGroup } from '../common/unit-or-group'

import DayList from './DayList'

interface Props {
  unitOrGroup: UnitOrGroup
  dailyStatistics: DayReservationStatisticsResult[]
}

export default React.memo(function ConfirmedDaysReservationList({
  unitOrGroup,
  dailyStatistics
}: Props) {
  const groupReservations = useMemo(
    () =>
      unitOrGroup.type === 'unit'
        ? dailyStatistics
        : dailyStatistics.map((day) => ({
            ...day,
            groupStatistics: day.groupStatistics.filter(
              (i) => i.groupId === unitOrGroup.id
            )
          })),
    [unitOrGroup, dailyStatistics]
  )

  return (
    <>
      <ContentArea
        opaque={false}
        paddingVertical="zero"
        paddingHorizontal="zero"
      >
        <DayList
          reservationStatistics={groupReservations}
          unitOrGroup={unitOrGroup}
        />
      </ContentArea>
    </>
  )
})
