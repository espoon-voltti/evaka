// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { renderResult } from 'employee-mobile-frontend/async-rendering'
import { useQueryResult } from 'lib-common/query'

import { UnitOrGroup } from '../common/unit-or-group'

import ConfirmedDaysReservationList from './ConfirmedDaysReservationList'
import { confirmedDaysReservationsStatisticsQuery } from './queries'

export default React.memo(function ConfirmedReservationsDaysWrapper({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const statisticsResult = useQueryResult(
    confirmedDaysReservationsStatisticsQuery({ unitId: unitOrGroup.unitId })
  )

  return (
    <>
      {renderResult(statisticsResult, (dayStatistics) => (
        <ConfirmedDaysReservationList
          dailyStatistics={dayStatistics}
          unitOrGroup={unitOrGroup}
        />
      ))}
    </>
  )
})
