// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.pedadocument.Attachment
*/
export interface Attachment {
  contentType: string
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.pedadocument.PedagogicalDocument
*/
export interface PedagogicalDocument {
  attachment: Attachment | null
  childId: UUID
  created: Date
  description: string
  id: UUID
  updated: Date
}

/**
* Generated from fi.espoo.evaka.pedadocument.PedagogicalDocumentPostBody
*/
export interface PedagogicalDocumentPostBody {
  attachmentId: UUID | null
  childId: UUID
  description: string
}
