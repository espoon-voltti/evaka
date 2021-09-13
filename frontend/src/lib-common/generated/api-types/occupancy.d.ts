// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import { UUID } from '../../types'

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