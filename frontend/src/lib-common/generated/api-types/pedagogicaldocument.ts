// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.Attachment
*/
export interface Attachment {
  contentType: string
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocument
*/
export interface PedagogicalDocument {
  attachment: Attachment | null
  childId: UUID
  created: Date
  description: string | null
  id: UUID
  updated: Date
}

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentCitizen
*/
export interface PedagogicalDocumentCitizen {
  attachment: Attachment | null
  childId: UUID
  created: Date
  description: string | null
  id: UUID
  isRead: boolean
}

/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentPostBody
*/
export interface PedagogicalDocumentPostBody {
  childId: UUID
  description: string
}
