// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faFileAlt } from 'lib-icons'

import { useTranslation } from '../../localization'
import { FinanceDecisionCitizenInfo } from 'lib-common/generated/api-types/application'
import { FinanceDecisionType } from 'lib-common/generated/api-types/invoicing'

interface Props {
  decisionData: FinanceDecisionCitizenInfo
  startOpen?: boolean
}

const getDecisionUrl = (decisionId: string, type: FinanceDecisionType) =>
  type === 'FEE_DECISION'
    ? `/api/application/citizen/fee-decisions/${decisionId}/download`
    : `/api/application/citizen/voucher-value-decisions/${decisionId}/download`

export default React.memo(function FinanceDecision({
  decisionData,
  startOpen = false
}: Props) {
  const t = useTranslation()
  const [open, setOpen] = useState(startOpen)
  const toggleOpen = useCallback(() => setOpen((o) => !o), [])
  const formatDuration = useCallback(() => `${decisionData.validFrom.format()} - ${decisionData.validTo?.format() && ''}`, [decisionData])

  return (
    <CollapsibleContentArea
      opaque={false}
      open={open}
      toggleOpen={toggleOpen}
      title={
        <H3
          noMargin
          data-qa={`title-decision-type-${decisionData.id}`}
          aria-label={
            `${t.decisions.financeDecisions.type[decisionData.type]
            } ${formatDuration()}`
          }
        >
          {t.decisions.financeDecisions.type[decisionData.type]} {` ${formatDuration()}`}
        </H3>
      }
      paddingHorizontal="0"
      paddingVertical="0"
      data-qa={`assistance-decision-${decisionData.id}`}
    >
      <Gap size="xs" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">

        <Label>{t.decisions.financeDecisions.validityPeriod}</Label>
        <span data-qa="validity-period">{formatDuration()}</span>

        <Label>{t.decisions.financeDecisions.sentAt}</Label>
        <span data-qa="sent-at">{decisionData.sentAt.format()}</span>

        <Label>{t.decisions.financeDecisions.liableCitizens}</Label>
        <span data-qa="liable-citizen-names">{decisionData.coDebtors.map(citizen => `${citizen.firstName} ${citizen.lastName}`).join(', ')}</span>
        {decisionData.decisionChildren.length > 0 && (
          <>
            <Label>{t.decisions.financeDecisions.voucherValueChild}</Label>
            <span data-qa="child-names">{decisionData.decisionChildren.map(child => `${child.firstName} ${child.lastName}`).join(', ')}</span>
          </>
        )
        }
      </ListGrid>
      <Gap size="m" />

      <InlineButton
        icon={faFileAlt}
        text={t.decisions.financeDecisions.loadDecisionPDF}
        onClick={() => window.open(
          getDecisionUrl(decisionData.id, decisionData.type),
          '_blank',
          'noopener,noreferrer'
        )}
      />

    </CollapsibleContentArea>
  )
})
