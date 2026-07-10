// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Chip } from 'lib-components/atoms/Chip'
import Tooltip from 'lib-components/atoms/Tooltip'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faExclamation, faFile } from 'lib-icons'

export interface DecisionReasoningChipsProps {
  individualReasoningCount: number
  reasoningWarningCount: number
  individualTooltip: string
  warningTooltip: string
}

export default React.memo(function DecisionReasoningChips({
  individualReasoningCount,
  reasoningWarningCount,
  individualTooltip,
  warningTooltip
}: DecisionReasoningChipsProps) {
  if (individualReasoningCount <= 0 && reasoningWarningCount <= 0) {
    return null
  }
  return (
    <FixedSpaceRow $spacing="xs">
      {individualReasoningCount > 0 && (
        <Tooltip tooltip={individualTooltip} width="large" hyphens="manual">
          <Chip
            label={String(individualReasoningCount)}
            icon={faFile}
            size="small"
            colorPalette="purple"
            data-qa="decision-reasoning-individual-count"
          />
        </Tooltip>
      )}
      {reasoningWarningCount > 0 && (
        <Tooltip tooltip={warningTooltip} width="large" hyphens="manual">
          <Chip
            label={String(reasoningWarningCount)}
            icon={faExclamation}
            iconCircle
            size="small"
            colorPalette="orange"
            data-qa="decision-reasoning-warning-count"
          />
        </Tooltip>
      )}
    </FixedSpaceRow>
  )
})
