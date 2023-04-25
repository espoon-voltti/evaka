// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTheme } from 'styled-components'

import type { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import { StaticChip } from 'lib-components/atoms/Chip'

export const AssistanceNeedDecisionStatusChip = React.memo(
  function AssistanceNeedDecisionStatusChip({
    decisionStatus,
    texts,
    'data-qa': dataQa
  }: {
    decisionStatus: AssistanceNeedDecisionStatus
    texts: Record<AssistanceNeedDecisionStatus, string>
    'data-qa'?: string
  }) {
    const theme = useTheme()

    const statusColor = {
      DRAFT: theme.colors.accents.a7mint,
      NEEDS_WORK: theme.colors.status.warning,
      ACCEPTED: theme.colors.accents.a3emerald,
      REJECTED: theme.colors.status.danger,
      ANNULLED: theme.colors.status.danger
    }[decisionStatus]

    const textColor =
      decisionStatus === 'ANNULLED' ? theme.colors.grayscale.g0 : undefined

    return (
      <StaticChip
        color={statusColor}
        textColor={textColor}
        fitContent
        data-qa={dataQa}
        data-qa-status={decisionStatus}
      >
        {texts[decisionStatus]}
      </StaticChip>
    )
  }
)
