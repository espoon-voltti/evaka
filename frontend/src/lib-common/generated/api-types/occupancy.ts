// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import { Caretakers } from './daycare'
import { JsonOf } from '../../json'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.occupancy.ChildCapacityPoint
*/
export interface ChildCapacityPoint {
  capacity: number
  time: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.occupancy.ChildOccupancyAttendance
*/
export interface ChildOccupancyAttendance {
  arrived: HelsinkiDateTime
  capacity: number
  childId: UUID
  departed: HelsinkiDateTime | null
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
  staffCapacity: number
  time: HelsinkiDateTime
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
* Generated from fi.espoo.evaka.occupancy.OccupancyType
*/
export type OccupancyType =
  | 'PLANNED'
  | 'CONFIRMED'
  | 'REALIZED'

/**
* Generated from fi.espoo.evaka.occupancy.OccupancyValues
*/
export interface OccupancyValues {
  caretakers: number | null
  headcount: number
  percentage: number | null
  sum: number
  sumOver3y: number
  sumUnder3y: number
}

/**
* Generated from fi.espoo.evaka.occupancy.RealtimeOccupancy
*/
export interface RealtimeOccupancy {
  childAttendances: ChildOccupancyAttendance[]
  childCapacitySumSeries: ChildCapacityPoint[]
  occupancySeries: OccupancyPoint[]
  staffAttendances: StaffOccupancyAttendance[]
  staffCapacitySumSeries: StaffCapacityPoint[]
}

/**
* Generated from fi.espoo.evaka.occupancy.StaffCapacityPoint
*/
export interface StaffCapacityPoint {
  capacity: number
  time: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.occupancy.StaffOccupancyAttendance
*/
export interface StaffOccupancyAttendance {
  arrived: HelsinkiDateTime
  capacity: number
  departed: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.occupancy.UnitOccupancies
*/
export interface UnitOccupancies {
  caretakers: Caretakers
  confirmed: OccupancyResponse
  planned: OccupancyResponse
  realized: OccupancyResponse
  realtime: RealtimeOccupancy | null
}


export function deserializeJsonChildCapacityPoint(json: JsonOf<ChildCapacityPoint>): ChildCapacityPoint {
  return {
    ...json,
    time: HelsinkiDateTime.parseIso(json.time)
  }
}


export function deserializeJsonChildOccupancyAttendance(json: JsonOf<ChildOccupancyAttendance>): ChildOccupancyAttendance {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
  }
}


export function deserializeJsonOccupancyPeriod(json: JsonOf<OccupancyPeriod>): OccupancyPeriod {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonOccupancyPoint(json: JsonOf<OccupancyPoint>): OccupancyPoint {
  return {
    ...json,
    time: HelsinkiDateTime.parseIso(json.time)
  }
}


export function deserializeJsonOccupancyResponse(json: JsonOf<OccupancyResponse>): OccupancyResponse {
  return {
    ...json,
    max: (json.max != null) ? deserializeJsonOccupancyPeriod(json.max) : null,
    min: (json.min != null) ? deserializeJsonOccupancyPeriod(json.min) : null,
    occupancies: json.occupancies.map(e => deserializeJsonOccupancyPeriod(e))
  }
}


export function deserializeJsonOccupancyResponseGroupLevel(json: JsonOf<OccupancyResponseGroupLevel>): OccupancyResponseGroupLevel {
  return {
    ...json,
    occupancies: deserializeJsonOccupancyResponse(json.occupancies)
  }
}


export function deserializeJsonRealtimeOccupancy(json: JsonOf<RealtimeOccupancy>): RealtimeOccupancy {
  return {
    ...json,
    childAttendances: json.childAttendances.map(e => deserializeJsonChildOccupancyAttendance(e)),
    childCapacitySumSeries: json.childCapacitySumSeries.map(e => deserializeJsonChildCapacityPoint(e)),
    occupancySeries: json.occupancySeries.map(e => deserializeJsonOccupancyPoint(e)),
    staffAttendances: json.staffAttendances.map(e => deserializeJsonStaffOccupancyAttendance(e)),
    staffCapacitySumSeries: json.staffCapacitySumSeries.map(e => deserializeJsonStaffCapacityPoint(e))
  }
}


export function deserializeJsonStaffCapacityPoint(json: JsonOf<StaffCapacityPoint>): StaffCapacityPoint {
  return {
    ...json,
    time: HelsinkiDateTime.parseIso(json.time)
  }
}


export function deserializeJsonStaffOccupancyAttendance(json: JsonOf<StaffOccupancyAttendance>): StaffOccupancyAttendance {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
  }
}


export function deserializeJsonUnitOccupancies(json: JsonOf<UnitOccupancies>): UnitOccupancies {
  return {
    ...json,
    confirmed: deserializeJsonOccupancyResponse(json.confirmed),
    planned: deserializeJsonOccupancyResponse(json.planned),
    realized: deserializeJsonOccupancyResponse(json.realized),
    realtime: (json.realtime != null) ? deserializeJsonRealtimeOccupancy(json.realtime) : null
  }
}
