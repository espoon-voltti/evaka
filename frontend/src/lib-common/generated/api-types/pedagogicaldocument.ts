// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Attachment } from './attachment'
import type { EvakaUser } from './user'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { PedagogicalDocumentId } from './shared'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocument
*/
export interface PedagogicalDocument {
  attachments: Attachment[]
  childId: PersonId
  createdAt: HelsinkiDateTime
  createdBy: EvakaUser
  description: string
  id: PedagogicalDocumentId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
}

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentCitizen
*/
export interface PedagogicalDocumentCitizen {
  attachments: Attachment[]
  childId: PersonId
  createdAt: HelsinkiDateTime
  description: string
  id: PedagogicalDocumentId
  isRead: boolean
}

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentPostBody
*/
export interface PedagogicalDocumentPostBody {
  childId: PersonId
  description: string
}


export function deserializeJsonPedagogicalDocument(json: JsonOf<PedagogicalDocument>): PedagogicalDocument {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonPedagogicalDocumentCitizen(json: JsonOf<PedagogicalDocumentCitizen>): PedagogicalDocumentCitizen {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
  }
}
