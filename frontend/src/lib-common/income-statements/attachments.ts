// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import partition from 'lodash/partition'

import { Attachment } from '../generated/api-types/attachment'
import {
  IncomeStatementAttachment,
  IncomeStatementAttachmentType
} from '../generated/api-types/incomestatement'
import { AttachmentId } from '../generated/api-types/shared'

export type AttachmentsByType = Partial<
  Record<IncomeStatementAttachmentType, Attachment[]>
>

export type IncomeStatementAttachments =
  | { typed: true; attachmentsByType: AttachmentsByType }
  | { typed: false; untypedAttachments: Attachment[] }

/**
 * Income statements can contain typed an untyped attachments. New code always adds a type to attachments,
 * but income statements created before the introduction of attachment types have untyped attachments.
 * An income statement has only typed or only untyped attachments.
 */
export function toIncomeStatementAttachments(
  attachments: IncomeStatementAttachment[]
): IncomeStatementAttachments {
  const [attachmentsWithType, untypedAttachments] = partition(
    attachments,
    (a) => a.type !== null
  )
  if (untypedAttachments.length !== 0) {
    return { typed: false, untypedAttachments }
  }

  const attachmentsByType: AttachmentsByType = groupBy(
    attachmentsWithType,
    'type'
  )
  return { typed: true, attachmentsByType }
}

export function numAttachments(
  incomeStatementAttachments: IncomeStatementAttachments
): number {
  return incomeStatementAttachments.typed
    ? Object.keys(incomeStatementAttachments.attachmentsByType).length
    : incomeStatementAttachments.untypedAttachments.length
}

export function collectAttachmentIds(
  incomeStatementAttachments: IncomeStatementAttachments
): AttachmentId[] {
  const attachments = incomeStatementAttachments.typed
    ? Object.values(incomeStatementAttachments.attachmentsByType).flat()
    : incomeStatementAttachments.untypedAttachments
  return attachments.map((a) => a.id)
}
