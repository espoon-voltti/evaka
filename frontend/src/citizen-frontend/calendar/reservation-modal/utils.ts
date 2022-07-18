// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import uniqBy from 'lodash/uniqBy'

import {
  DailyReservationData,
  TimeRange
} from 'lib-common/generated/api-types/reservations'
import { TimeRanges } from 'lib-common/reservations'

export const emptyTimeRange: [TimeRange] = [
  {
    startTime: '',
    endTime: ''
  }
]

export const hasReservationsForEveryChild = (
  dayData: DailyReservationData[],
  childIds: string[]
) =>
  dayData.every((reservations) =>
    childIds.every((childId) =>
      reservations.children.some(
        (child) => child.childId === childId && child.reservations.length > 0
      )
    )
  )

export const allChildrenAreAbsent = (
  dayData: DailyReservationData[],
  childIds: string[]
) =>
  dayData.every((reservations) =>
    childIds.every((childId) =>
      reservations.children.some(
        (child) => child.childId === childId && !!child.absence
      )
    )
  )

export const allChildrenHaveDayOff = (
  dayData: DailyReservationData[],
  childIds: string[]
) =>
  dayData.every((reservations) =>
    childIds.every((childId) =>
      reservations.children.some(
        (child) => child.childId === childId && !!child.dayOff
      )
    )
  )

export const bindUnboundedTimeRanges = (ranges: TimeRange[]): TimeRanges => {
  if (ranges.length === 1) {
    return ranges as [TimeRange]
  }

  if (ranges.length === 2) {
    return ranges as [TimeRange, TimeRange]
  }

  throw Error(`${ranges.length} time ranges when 1-2 expected`)
}

export const getCommonTimeRanges = (
  dayData: DailyReservationData[],
  childIds: string[]
): TimeRange[] | undefined => {
  const uniqueRanges = uniqBy(
    dayData
      .flatMap((reservations) =>
        reservations.children
          .filter(({ childId }) => childIds.includes(childId))
          .map((child) => child.reservations)
      )
      .filter((ranges) => ranges.length > 0),
    (times) => JSON.stringify(times)
  )

  if (uniqueRanges.length === 1) {
    return uniqueRanges[0]
  }

  return undefined
}
