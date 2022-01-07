// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceBasisOption
*/
export interface AssistanceBasisOption {
  descriptionFi: string | null
  nameFi: string
  value: string
}

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeed
*/
export interface AssistanceNeed {
  bases: string[]
  capacityFactor: number
  childId: UUID
  endDate: LocalDate
  id: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceneed.AssistanceNeedRequest
*/
export interface AssistanceNeedRequest {
  bases: string[]
  capacityFactor: number
  endDate: LocalDate
  startDate: LocalDate
}
