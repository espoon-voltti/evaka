// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from '../../local-date'
import { Action } from '../action'
import { JsonOf } from '../../json'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceAction
*/
export interface AssistanceAction {
  actions: string[]
  childId: UUID
  endDate: LocalDate
  id: UUID
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
  otherAction: string
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceActionResponse
*/
export interface AssistanceActionResponse {
  action: AssistanceAction
  permittedActions: Action.AssistanceAction[]
}


export function deserializeJsonAssistanceAction(json: JsonOf<AssistanceAction>): AssistanceAction {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonAssistanceActionRequest(json: JsonOf<AssistanceActionRequest>): AssistanceActionRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonAssistanceActionResponse(json: JsonOf<AssistanceActionResponse>): AssistanceActionResponse {
  return {
    ...json,
    action: deserializeJsonAssistanceAction(json.action)
  }
}
