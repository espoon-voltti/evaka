// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from '../../date-range'
import LocalDate from '../../local-date'
import TimeRange from '../../time-range'
import { Action } from '../action'
import { JsonOf } from '../../json'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimeNotification
*/
export interface DailyServiceTimeNotification {
  dateFrom: LocalDate
  hasDeletedReservations: boolean
  id: UUID
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
*/
export interface DailyServiceTimes {
  childId: UUID
  id: UUID
  times: DailyServiceTimesValue
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.DailyServiceTimesEndDate
*/
export interface DailyServiceTimesEndDate {
  endDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.DailyServiceTimesResponse
*/
export interface DailyServiceTimesResponse {
  dailyServiceTimes: DailyServiceTimes
  permittedActions: Action.DailyServiceTime[]
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
*/
export type DailyServiceTimesType =
  | 'REGULAR'
  | 'IRREGULAR'
  | 'VARIABLE_TIME'


export namespace DailyServiceTimesValue {
  /**
  * Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue.IrregularTimes
  */
  export interface IrregularTimes {
    type: 'IRREGULAR'
    friday: TimeRange | null
    monday: TimeRange | null
    saturday: TimeRange | null
    sunday: TimeRange | null
    thursday: TimeRange | null
    tuesday: TimeRange | null
    validityPeriod: DateRange
    wednesday: TimeRange | null
  }

  /**
  * Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue.RegularTimes
  */
  export interface RegularTimes {
    type: 'REGULAR'
    regularTimes: TimeRange
    validityPeriod: DateRange
  }

  /**
  * Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue.VariableTimes
  */
  export interface VariableTimes {
    type: 'VARIABLE_TIME'
    validityPeriod: DateRange
  }
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
*/
export type DailyServiceTimesValue = DailyServiceTimesValue.IrregularTimes | DailyServiceTimesValue.RegularTimes | DailyServiceTimesValue.VariableTimes



export function deserializeJsonDailyServiceTimeNotification(json: JsonOf<DailyServiceTimeNotification>): DailyServiceTimeNotification {
  return {
    ...json,
    dateFrom: LocalDate.parseIso(json.dateFrom)
  }
}


export function deserializeJsonDailyServiceTimes(json: JsonOf<DailyServiceTimes>): DailyServiceTimes {
  return {
    ...json,
    times: deserializeJsonDailyServiceTimesValue(json.times)
  }
}


export function deserializeJsonDailyServiceTimesEndDate(json: JsonOf<DailyServiceTimesEndDate>): DailyServiceTimesEndDate {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null
  }
}


export function deserializeJsonDailyServiceTimesResponse(json: JsonOf<DailyServiceTimesResponse>): DailyServiceTimesResponse {
  return {
    ...json,
    dailyServiceTimes: deserializeJsonDailyServiceTimes(json.dailyServiceTimes)
  }
}



export function deserializeJsonDailyServiceTimesValueIrregularTimes(json: JsonOf<DailyServiceTimesValue.IrregularTimes>): DailyServiceTimesValue.IrregularTimes {
  return {
    ...json,
    friday: (json.friday != null) ? TimeRange.parseJson(json.friday) : null,
    monday: (json.monday != null) ? TimeRange.parseJson(json.monday) : null,
    saturday: (json.saturday != null) ? TimeRange.parseJson(json.saturday) : null,
    sunday: (json.sunday != null) ? TimeRange.parseJson(json.sunday) : null,
    thursday: (json.thursday != null) ? TimeRange.parseJson(json.thursday) : null,
    tuesday: (json.tuesday != null) ? TimeRange.parseJson(json.tuesday) : null,
    validityPeriod: DateRange.parseJson(json.validityPeriod),
    wednesday: (json.wednesday != null) ? TimeRange.parseJson(json.wednesday) : null
  }
}

export function deserializeJsonDailyServiceTimesValueRegularTimes(json: JsonOf<DailyServiceTimesValue.RegularTimes>): DailyServiceTimesValue.RegularTimes {
  return {
    ...json,
    regularTimes: TimeRange.parseJson(json.regularTimes),
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}

export function deserializeJsonDailyServiceTimesValueVariableTimes(json: JsonOf<DailyServiceTimesValue.VariableTimes>): DailyServiceTimesValue.VariableTimes {
  return {
    ...json,
    validityPeriod: DateRange.parseJson(json.validityPeriod)
  }
}
export function deserializeJsonDailyServiceTimesValue(json: JsonOf<DailyServiceTimesValue>): DailyServiceTimesValue {
  switch (json.type) {
    case 'IRREGULAR': return deserializeJsonDailyServiceTimesValueIrregularTimes(json)
    case 'REGULAR': return deserializeJsonDailyServiceTimesValueRegularTimes(json)
    case 'VARIABLE_TIME': return deserializeJsonDailyServiceTimesValueVariableTimes(json)
    default: return json
  }
}
