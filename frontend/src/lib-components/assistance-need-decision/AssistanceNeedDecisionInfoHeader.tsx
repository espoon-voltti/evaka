// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { AssistanceNeedDecisionStatus } from 'lib-common/generated/api-types/assistanceneed'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Strong } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { AssistanceNeedDecisionStatusChip } from './AssistanceNeedDecisionStatusChip'

export default React.memo(function AssistanceNeedDecisionInfoHeader({
  decisionNumber,
  decisionStatus,
  texts: t
}: {
  decisionNumber: number
  decisionStatus: AssistanceNeedDecisionStatus
  texts: {
    decisionNumber: string
    statuses: Record<AssistanceNeedDecisionStatus, string>
    confidential: string
    lawReference: string
  }
}) {
  return (
    <FixedSpaceColumn spacing={defaultMargins.s}>
      <span>
        {t.decisionNumber}{' '}
        <span data-qa="decision-number">{decisionNumber}</span>
      </span>
      <AssistanceNeedDecisionStatusChip
        decisionStatus={decisionStatus}
        texts={t.statuses}
        data-qa="decision-status"
      />
      <FixedSpaceColumn spacing={defaultMargins.xs}>
        <Strong>{t.confidential}</Strong>
        <span>{t.lawReference}</span>
      </FixedSpaceColumn>
    </FixedSpaceColumn>
  )
})
