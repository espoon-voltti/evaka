// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import type LocalDate from '../../local-date'
import type { Action } from '../action'
import type { DailyServiceTimes } from '../../api-types/child/common'
import type { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimeNotification
*/
export interface DailyServiceTimeNotification {
  dateFrom: LocalDate
  hasDeletedReservations: boolean
  id: UUID
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesController.DailyServiceTimesResponse
*/
export interface DailyServiceTimesResponse {
  dailyServiceTimes: DailyServiceTimesWithId
  permittedActions: Action.DailyServiceTime[]
}

/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesWithId
*/
export interface DailyServiceTimesWithId {
  id: UUID
  times: DailyServiceTimes
}
