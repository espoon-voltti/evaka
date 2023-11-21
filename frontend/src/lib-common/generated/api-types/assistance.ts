// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import { Action } from '../action'
import { AssistanceActionResponse } from './assistanceaction'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.assistance.AssistanceFactor
*/
export interface AssistanceFactor {
  capacityFactor: number
  childId: UUID
  id: UUID
  modified: HelsinkiDateTime
  modifiedBy: string
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.AssistanceFactorResponse
*/
export interface AssistanceFactorResponse {
  data: AssistanceFactor
  permittedActions: Action.AssistanceFactor[]
}

/**
* Generated from fi.espoo.evaka.assistance.AssistanceFactorUpdate
*/
export interface AssistanceFactorUpdate {
  capacityFactor: number
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.AssistanceResponse
*/
export interface AssistanceResponse {
  assistanceActions: AssistanceActionResponse[]
  assistanceFactors: AssistanceFactorResponse[]
  daycareAssistances: DaycareAssistanceResponse[]
  otherAssistanceMeasures: OtherAssistanceMeasureResponse[]
  preschoolAssistances: PreschoolAssistanceResponse[]
}

/**
* Generated from fi.espoo.evaka.assistance.DaycareAssistance
*/
export interface DaycareAssistance {
  childId: UUID
  id: UUID
  level: DaycareAssistanceLevel
  modified: HelsinkiDateTime
  modifiedBy: string
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.DaycareAssistanceLevel
*/
export const daycareAssistanceLevels = [
  'GENERAL_SUPPORT',
  'GENERAL_SUPPORT_WITH_DECISION',
  'INTENSIFIED_SUPPORT',
  'SPECIAL_SUPPORT'
] as const

export type DaycareAssistanceLevel = typeof daycareAssistanceLevels[number]

/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.DaycareAssistanceResponse
*/
export interface DaycareAssistanceResponse {
  data: DaycareAssistance
  permittedActions: Action.DaycareAssistance[]
}

/**
* Generated from fi.espoo.evaka.assistance.DaycareAssistanceUpdate
*/
export interface DaycareAssistanceUpdate {
  level: DaycareAssistanceLevel
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.OtherAssistanceMeasure
*/
export interface OtherAssistanceMeasure {
  childId: UUID
  id: UUID
  modified: HelsinkiDateTime
  modifiedBy: string
  type: OtherAssistanceMeasureType
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.OtherAssistanceMeasureResponse
*/
export interface OtherAssistanceMeasureResponse {
  data: OtherAssistanceMeasure
  permittedActions: Action.OtherAssistanceMeasure[]
}

/**
* Generated from fi.espoo.evaka.assistance.OtherAssistanceMeasureType
*/
export const otherAssistanceMeasureTypes = [
  'TRANSPORT_BENEFIT',
  'ACCULTURATION_SUPPORT',
  'ANOMALOUS_EDUCATION_START',
  'CHILD_DISCUSSION_OFFERED',
  'CHILD_DISCUSSION_HELD',
  'CHILD_DISCUSSION_COUNSELING'
] as const

export type OtherAssistanceMeasureType = typeof otherAssistanceMeasureTypes[number]

/**
* Generated from fi.espoo.evaka.assistance.OtherAssistanceMeasureUpdate
*/
export interface OtherAssistanceMeasureUpdate {
  type: OtherAssistanceMeasureType
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.PreschoolAssistance
*/
export interface PreschoolAssistance {
  childId: UUID
  id: UUID
  level: PreschoolAssistanceLevel
  modified: HelsinkiDateTime
  modifiedBy: string
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistance.PreschoolAssistanceLevel
*/
export const preschoolAssistanceLevels = [
  'INTENSIFIED_SUPPORT',
  'SPECIAL_SUPPORT',
  'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1',
  'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2'
] as const

export type PreschoolAssistanceLevel = typeof preschoolAssistanceLevels[number]

/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.PreschoolAssistanceResponse
*/
export interface PreschoolAssistanceResponse {
  data: PreschoolAssistance
  permittedActions: Action.PreschoolAssistance[]
}

/**
* Generated from fi.espoo.evaka.assistance.PreschoolAssistanceUpdate
*/
export interface PreschoolAssistanceUpdate {
  level: PreschoolAssistanceLevel
  validDuring: FiniteDateRange
}
