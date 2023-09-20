// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTheme } from 'styled-components'

import { DocumentStatus } from 'lib-common/generated/api-types/document'
import { StaticChip } from 'lib-components/atoms/Chip'

import { useTranslations } from '../i18n'

interface StateChipProps {
  status: DocumentStatus
}

export function ChildDocumentStateChip({ status }: StateChipProps) {
  const i18n = useTranslations()
  const colors = useTheme().colors

  const statusColors: Record<DocumentStatus, string> = {
    DRAFT: colors.accents.a7mint,
    PREPARED: colors.accents.a4violet,
    COMPLETED: colors.grayscale.g15
  }

  return (
    <StaticChip color={statusColors[status]} data-qa="document-state-chip">
      {i18n.documentTemplates.documentStates[status]}
    </StaticChip>
  )
}
