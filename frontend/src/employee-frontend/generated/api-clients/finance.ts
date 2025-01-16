// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { FinanceNote } from 'lib-common/generated/api-types/finance'
import { FinanceNoteId } from 'lib-common/generated/api-types/shared'
import { FinanceNoteRequest } from 'lib-common/generated/api-types/finance'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { deserializeJsonFinanceNote } from 'lib-common/generated/api-types/finance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteController.createNote
*/
export async function createNote(
  request: {
    body: FinanceNoteRequest
  }
): Promise<FinanceNote> {
  const { data: json } = await client.request<JsonOf<FinanceNote>>({
    url: uri`/employee/note/finance/`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FinanceNoteRequest>
  })
  return deserializeJsonFinanceNote(json)
}


/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteController.deleteNote
*/
export async function deleteNote(
  request: {
    noteId: FinanceNoteId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/note/finance/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}
