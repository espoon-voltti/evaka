// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import LocalDate from '../../local-date'
import { Action } from '../action'
import { DailyServiceTimesValue } from '../../api-types/child/common'
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
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.DailyServiceTimesResponse
*/
export interface DailyServiceTimesResponse {
  dailyServiceTimes: DailyServiceTimes
  permittedActions: Action.DailyServiceTime[]
}
