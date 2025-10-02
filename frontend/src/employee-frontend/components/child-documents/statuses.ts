// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ChildDocumentDecisionStatus,
  DocumentStatus,
  ChildDocumentType
} from 'lib-common/generated/api-types/document'
import { getDocumentCategory } from 'lib-components/document-templates/documents'

const statusesByType: Record<ChildDocumentType, DocumentStatus[]> = {
  PEDAGOGICAL_ASSESSMENT: ['DRAFT', 'COMPLETED'],
  PEDAGOGICAL_REPORT: ['DRAFT', 'COMPLETED'],
  HOJKS: ['DRAFT', 'PREPARED', 'COMPLETED'],
  MIGRATED_VASU: ['COMPLETED'],
  MIGRATED_LEOPS: ['COMPLETED'],
  MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION: ['COMPLETED'],
  MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION: ['COMPLETED'],
  VASU: ['DRAFT', 'PREPARED', 'COMPLETED'],
  LEOPS: ['DRAFT', 'PREPARED', 'COMPLETED'],
  CITIZEN_BASIC: ['DRAFT', 'CITIZEN_DRAFT', 'COMPLETED'],
  OTHER_DECISION: ['DRAFT', 'DECISION_PROPOSAL', 'COMPLETED'],
  OTHER: ['DRAFT', 'COMPLETED']
}

export const getNextDocumentStatus = (
  type: ChildDocumentType,
  current: DocumentStatus
): DocumentStatus | null => {
  const statuses = statusesByType[type]
  const index = statuses.indexOf(current)
  if (index < 0) return null

  const nextStatus = index < statuses.length - 1 ? statuses[index + 1] : null

  if (
    getDocumentCategory(type) === 'decision' &&
    (nextStatus === 'DECISION_PROPOSAL' || nextStatus === 'COMPLETED')
  ) {
    // Transitioning a decision to DECISION_PROPOSAL or COMPLETED must be done through separate endpoints
    return null
  }

  return nextStatus
}

export const getPrevDocumentStatus = (
  type: ChildDocumentType,
  current: DocumentStatus
): DocumentStatus | null => {
  const statuses = statusesByType[type]
  const index = statuses.indexOf(current)
  if (index < 0) return null

  if (getDocumentCategory(type) === 'decision' && current === 'COMPLETED') {
    return null // Decisions cannot be cancelled
  }

  return index > 0 ? statuses[index - 1] : null
}

export const isChildDocumentEditable = (status: DocumentStatus) => {
  switch (status) {
    case 'DRAFT':
      return true
    case 'PREPARED':
      return true
    case 'CITIZEN_DRAFT':
      return false
    case 'DECISION_PROPOSAL':
      return false
    case 'COMPLETED':
      return false
  }
}

export const isChildDocumentPublishable = (
  type: ChildDocumentType,
  status: DocumentStatus
) =>
  status !== 'COMPLETED' &&
  type !== 'CITIZEN_BASIC' &&
  type !== 'OTHER_DECISION' &&
  type !== 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION' &&
  type !== 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'

export const isChildDocumentDecidable = (
  type: ChildDocumentType,
  status: DocumentStatus
) => {
  return (
    getDocumentCategory(type) === 'decision' && status === 'DECISION_PROPOSAL'
  )
}

export const isChildDocumentAnnullable = (
  type: ChildDocumentType,
  status: DocumentStatus,
  decisionStatus: ChildDocumentDecisionStatus | null
) => {
  return (
    getDocumentCategory(type) === 'decision' &&
    status === 'COMPLETED' &&
    decisionStatus === 'ACCEPTED'
  )
}
