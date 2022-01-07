// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.attachment.AttachmentType
*/
export type AttachmentType = 
  | 'URGENCY'
  | 'EXTENDED_CARE'

/**
* Generated from fi.espoo.evaka.attachment.MessageAttachment
*/
export interface MessageAttachment {
  contentType: string
  id: UUID
  name: string
}
