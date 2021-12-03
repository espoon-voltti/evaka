// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceAction
*/
export interface AssistanceAction {
  actions: string[]
  childId: UUID
  endDate: LocalDate
  id: UUID
  measures: AssistanceMeasure[]
  otherAction: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceActionOption
*/
export interface AssistanceActionOption {
  descriptionFi: string | null
  nameFi: string
  value: string
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceActionRequest
*/
export interface AssistanceActionRequest {
  actions: string[]
  endDate: LocalDate
  measures: AssistanceMeasure[]
  otherAction: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceMeasure
*/
export type AssistanceMeasure = 
  | 'SPECIAL_ASSISTANCE_DECISION'
  | 'INTENSIFIED_ASSISTANCE'
  | 'EXTENDED_COMPULSORY_EDUCATION'
  | 'CHILD_SERVICE'
  | 'CHILD_ACCULTURATION_SUPPORT'
  | 'TRANSPORT_BENEFIT'
