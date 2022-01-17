// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.occupancy.ChildCapacityPoint
*/
export interface ChildCapacityPoint {
  capacity: number
  time: Date
}

/**
* Generated from fi.espoo.evaka.occupancy.ChildOccupancyAttendance
*/
export interface ChildOccupancyAttendance {
  arrived: Date
  capacity: number
  departed: Date | null
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyPeriod
*/
export interface OccupancyPeriod {
  caretakers: number | null
  headcount: number
  percentage: number | null
  period: FiniteDateRange
  sum: number
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyPoint
*/
export interface OccupancyPoint {
  childCapacity: number
  occupancyRatio: number | null
  staffCount: number
  time: Date
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyResponse
*/
export interface OccupancyResponse {
  max: OccupancyPeriod | null
  min: OccupancyPeriod | null
  occupancies: OccupancyPeriod[]
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyResponseGroupLevel
*/
export interface OccupancyResponseGroupLevel {
  groupId: UUID
  occupancies: OccupancyResponse
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyResponseSpeculated
*/
export interface OccupancyResponseSpeculated {
  max3Months: OccupancyValues | null
  max3MonthsSpeculated: OccupancyValues | null
  max6Months: OccupancyValues | null
  max6MonthsSpeculated: OccupancyValues | null
}

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyValues
*/
export interface OccupancyValues {
  caretakers: number | null
  headcount: number
  percentage: number | null
  sum: number
}

/**
* Generated from fi.espoo.evaka.occupancy.RealtimeOccupancy
*/
export interface RealtimeOccupancy {
  childAttendances: ChildOccupancyAttendance[]
  childCapacitySumSeries: ChildCapacityPoint[]
  occupancySeries: OccupancyPoint[]
  staffAttendances: StaffOccupancyAttendance[]
  staffCountSeries: StaffCountPoint[]
}

/**
* Generated from fi.espoo.evaka.occupancy.StaffCountPoint
*/
export interface StaffCountPoint {
  count: number
  time: Date
}

/**
* Generated from fi.espoo.evaka.occupancy.StaffOccupancyAttendance
*/
export interface StaffOccupancyAttendance {
  arrived: Date
  departed: Date | null
}
