// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AttachmentId } from './shared'

/**
* Generated from fi.espoo.evaka.attachment.AttachmentType
*/
export type AttachmentType =
  | 'URGENCY'
  | 'EXTENDED_CARE'
  | 'SERVICE_WORKER_ATTACHMENT'

/**
* Generated from fi.espoo.evaka.attachment.IncomeAttachment
*/
export interface IncomeAttachment {
  contentType: string
  id: AttachmentId
  name: string
}

/**
* Generated from fi.espoo.evaka.attachment.MessageAttachment
*/
export interface MessageAttachment {
  contentType: string
  id: AttachmentId
  name: string
}
