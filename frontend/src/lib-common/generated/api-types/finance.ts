// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { EvakaUserId } from './shared'
import type { FinanceNoteId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNote
*/
export interface FinanceNote {
  content: string
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  createdByName: string
  id: FinanceNoteId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUserId
  modifiedByName: string
}

/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteRequest
*/
export interface FinanceNoteRequest {
  content: string
  personId: PersonId
}

/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteResponse
*/
export interface FinanceNoteResponse {
  note: FinanceNote
  permittedActions: Action.FinanceNote[]
}


export function deserializeJsonFinanceNote(json: JsonOf<FinanceNote>): FinanceNote {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonFinanceNoteResponse(json: JsonOf<FinanceNoteResponse>): FinanceNoteResponse {
  return {
    ...json,
    note: deserializeJsonFinanceNote(json.note)
  }
}
