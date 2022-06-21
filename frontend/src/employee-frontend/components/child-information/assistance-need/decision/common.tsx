// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import { StaticChip } from 'lib-components/atoms/Chip'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Strong } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { theme } from 'lib-customizations/common'

export const FooterContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: ${defaultMargins.s};
`

export const AssistanceNeedDecisionStatusChip = React.memo(
  function AssistanceNeedDecisionStatusChip({
    decisionStatus,
    'data-qa': dataQa
  }: {
    decisionStatus: AssistanceNeedDecisionStatus
    'data-qa'?: string
  }) {
    const { i18n } = useTranslation()
    const statusColor = {
      DRAFT: theme.colors.accents.a7mint,
      NEEDS_WORK: theme.colors.status.warning,
      ACCEPTED: theme.colors.accents.a3emerald,
      REJECTED: theme.colors.status.danger
    }

    return (
      <StaticChip
        color={statusColor[decisionStatus]}
        fitContent
        data-qa={dataQa}
      >
        {i18n.childInformation.assistanceNeedDecision.statuses[decisionStatus]}
      </StaticChip>
    )
  }
)

export const DecisionInfoHeader = React.memo(
  function AssistanceNeedDecisionStatus({
    decisionNumber,
    decisionStatus
  }: {
    decisionNumber: number
    decisionStatus: AssistanceNeedDecisionStatus
  }) {
    const { i18n } = useTranslation()

    return (
      <FixedSpaceColumn spacing={defaultMargins.s}>
        <span>
          {i18n.childInformation.assistanceNeedDecision.decisionNumber}{' '}
          <span data-qa="decision-number">{decisionNumber}</span>
        </span>
        <FixedSpaceColumn spacing={defaultMargins.xs}>
          <AssistanceNeedDecisionStatusChip
            decisionStatus={decisionStatus}
            data-qa="decision-status"
          />
          <Strong>
            {i18n.childInformation.assistanceNeedDecision.confidential}
          </Strong>
          <span>
            {i18n.childInformation.assistanceNeedDecision.lawReference}
          </span>
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    )
  }
)
