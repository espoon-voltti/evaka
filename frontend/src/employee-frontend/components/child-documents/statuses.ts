// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  DocumentStatus,
  DocumentType
} from 'lib-common/generated/api-types/document'

const statusesByType: Record<DocumentType, DocumentStatus[]> = {
  PEDAGOGICAL_ASSESSMENT: ['DRAFT', 'COMPLETED'],
  PEDAGOGICAL_REPORT: ['DRAFT', 'COMPLETED'],
  HOJKS: ['DRAFT', 'PREPARED', 'COMPLETED'],
  MIGRATED_VASU: ['COMPLETED'],
  MIGRATED_LEOPS: ['COMPLETED'],
  VASU: ['DRAFT', 'PREPARED', 'COMPLETED'],
  LEOPS: ['DRAFT', 'PREPARED', 'COMPLETED'],
  OTHER: ['DRAFT', 'COMPLETED']
}

export const getNextDocumentStatus = (
  type: DocumentType,
  current: DocumentStatus
): DocumentStatus | null => {
  const statuses = statusesByType[type]
  const index = statuses.indexOf(current)
  if (index < 0) return null

  return index < statuses.length - 1 ? statuses[index + 1] : null
}

export const getPrevDocumentStatus = (
  type: DocumentType,
  current: DocumentStatus
): DocumentStatus | null => {
  const statuses = statusesByType[type]
  const index = statuses.indexOf(current)
  if (index < 0) return null

  return index > 0 ? statuses[index - 1] : null
}
