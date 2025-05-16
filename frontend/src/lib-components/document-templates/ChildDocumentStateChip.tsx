// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTheme } from 'styled-components'

import type {
  ChildDocumentDecisionStatus,
  DocumentStatus
} from 'lib-common/generated/api-types/document'
import { StaticChip } from 'lib-components/atoms/Chip'

import { useTranslations } from '../i18n'

interface StateChipProps {
  status: DocumentStatus | ChildDocumentDecisionStatus
}

export function ChildDocumentStateChip({ status }: StateChipProps) {
  const i18n = useTranslations()
  const colors = useTheme().colors

  const statusColors: Record<
    DocumentStatus | ChildDocumentDecisionStatus,
    string
  > = {
    DRAFT: colors.accents.a7mint,
    PREPARED: colors.accents.a4violet,
    CITIZEN_DRAFT: colors.accents.a5orangeLight,
    DECISION_PROPOSAL: colors.accents.a4violet,
    COMPLETED: colors.grayscale.g15,
    ACCEPTED: colors.accents.a3emerald,
    REJECTED: colors.status.danger,
    ANNULLED: colors.status.danger
  }

  return (
    <StaticChip
      color={statusColors[status]}
      fitContent
      data-qa="document-state-chip"
    >
      {i18n.documentTemplates.documentStates[status]}
    </StaticChip>
  )
}
