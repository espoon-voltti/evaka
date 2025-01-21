// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { FinanceNote } from 'lib-common/generated/api-types/finance'
import { FinanceNoteId } from 'lib-common/generated/api-types/shared'
import { FinanceNoteRequest } from 'lib-common/generated/api-types/finance'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { deserializeJsonFinanceNote } from 'lib-common/generated/api-types/finance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteController.createFinanceNote
*/
export async function createFinanceNote(
  request: {
    body: FinanceNoteRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-notes/`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FinanceNoteRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteController.deleteFinanceNote
*/
export async function deleteFinanceNote(
  request: {
    noteId: FinanceNoteId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-notes/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteController.getFinanceNotes
*/
export async function getFinanceNotes(
  request: {
    id: PersonId
  }
): Promise<FinanceNote[]> {
  const { data: json } = await client.request<JsonOf<FinanceNote[]>>({
    url: uri`/employee/finance-notes/${request.id}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFinanceNote(e))
}


/**
* Generated from fi.espoo.evaka.finance.notes.FinanceNoteController.updateFinanceNote
*/
export async function updateFinanceNote(
  request: {
    noteId: FinanceNoteId,
    body: FinanceNoteRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-notes/${request.noteId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FinanceNoteRequest>
  })
  return json
}
