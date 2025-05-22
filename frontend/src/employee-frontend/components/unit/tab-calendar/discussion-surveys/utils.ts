// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import orderBy from 'lodash/orderBy'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'

export const getCombinedChildPlacementsForGroup = (
  placements: DaycarePlacementWithDetails[],
  groupId: GroupId
) => {
  const validPlacements = placements.filter((p) =>
    p.groupPlacements.some((gp) => gp.groupId === groupId)
  )
  return Object.values(groupBy(validPlacements, (p) => p.child.id)).map(
    (placements) => ({
      child: placements[0].child,
      groupPlacements: placements
        .flatMap((p) => p.groupPlacements)
        .filter((gp) => gp.groupId === groupId)
        .map((gp) => new DateRange(gp.startDate, gp.endDate))
    })
  )
}

export const getPeriodFromDatesOrToday = (
  dates: LocalDate[]
): FiniteDateRange => {
  const today = LocalDate.todayInSystemTz()
  const sortedDates = orderBy(dates)
  return sortedDates.length > 0
    ? new FiniteDateRange(sortedDates[0], sortedDates[sortedDates.length - 1])
    : new FiniteDateRange(today, today)
}
