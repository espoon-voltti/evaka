// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { AssistanceActionId } from './shared'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.assistanceaction.AssistanceAction
*/
export interface AssistanceAction {
  actions: string[]
  childId: PersonId
  endDate: LocalDate
  id: AssistanceActionId
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
