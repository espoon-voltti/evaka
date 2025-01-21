// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { EvakaUser } from './user'
import { FinanceNoteId } from './shared'
import { JsonOf } from '../../json'
import { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNote
*/
export interface FinanceNote {
  content: string
  createdAt: HelsinkiDateTime
  createdBy: EvakaUser
  id: FinanceNoteId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
}

/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteRequest
*/
export interface FinanceNoteRequest {
  content: string
  personId: PersonId
}


export function deserializeJsonFinanceNote(json: JsonOf<FinanceNote>): FinanceNote {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}
