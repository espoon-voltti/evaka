// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  getOccupancyPeriods,
  getOccupancyPeriodsOnGroups
} from 'employee-mobile-frontend/generated/api-clients/occupancy'
import { Result, wrapResult } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'

const getOccupancyPeriodsResult = wrapResult(getOccupancyPeriods)
const getOccupancyPeriodsOnGroupsResult = wrapResult(
  getOccupancyPeriodsOnGroups
)
export async function getRealizedOccupancyToday(
  unitId: string,
  groupId: string | undefined
): Promise<Result<number>> {
  const today = LocalDate.todayInSystemTz()
  if (groupId) {
    return (
      await getOccupancyPeriodsOnGroupsResult({
        unitId,
        from: today,
        to: today,
        type: 'REALIZED'
      })
    ).map((response) => {
      // There's only one occupancy, because we only fetch today's occupancy, so we can use `min`
      const percentage = response.find((group) => group.groupId === groupId)
        ?.occupancies.min?.percentage
      return percentage ?? 0
    })
  } else {
    return (
      await getOccupancyPeriodsResult({
        unitId,
        from: today,
        to: today,
        type: 'REALIZED'
      })
    ).map((response) => response.min?.percentage ?? 0)
  }
}
