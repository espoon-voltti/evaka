// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import type { FinanceDecisionCitizenInfo } from 'lib-common/generated/api-types/application'
import type { FinanceDecisionType } from 'lib-common/generated/api-types/invoicing'
import { fromUuid } from 'lib-common/id-type'
import { formatPersonName } from 'lib-common/names'
import { Button } from 'lib-components/atoms/buttons/Button'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { faFileAlt } from 'lib-icons'

import {
  downloadFeeDecisionPdf,
  downloadVoucherValueDecisionPdf
} from '../../generated/api-clients/application'
import { useTranslation } from '../../localization'
import { MetadataSection } from '../../metadata/MetadataSection'
import {
  feeDecisionMetadataQuery,
  voucherValueDecisionMetadataQuery
} from '../queries'

interface Props {
  decisionData: FinanceDecisionCitizenInfo
  startOpen?: boolean
}

const getDecisionUrl = (decisionId: string, type: FinanceDecisionType) =>
  type === 'FEE_DECISION'
    ? downloadFeeDecisionPdf({ id: fromUuid(decisionId) }).url.toString()
    : downloadVoucherValueDecisionPdf({
        id: fromUuid(decisionId)
      }).url.toString()

const getMetadataQuery = (decisionId: string, type: FinanceDecisionType) =>
  type === 'FEE_DECISION'
    ? feeDecisionMetadataQuery({
        feeDecisionId: fromUuid(decisionId)
      })
    : voucherValueDecisionMetadataQuery({
        voucherValueDecisionId: fromUuid(decisionId)
      })

export default React.memo(function FinanceDecision({
  decisionData,
  startOpen = false
}: Props) {
  const t = useTranslation()
  const [open, setOpen] = useState(startOpen)
  const toggleOpen = useCallback(() => setOpen((o) => !o), [])
  const formatFullDuration = useCallback(
    () =>
      `${decisionData.validFrom.format()} - ${
        decisionData.validTo ? decisionData.validTo.format() : ''
      }`,
    [decisionData]
  )
  const formatTitleDuration = useCallback(
    () => `${decisionData.validFrom.format()} -`,
    [decisionData]
  )

  return (
    <CollapsibleContentArea
      data-qa={`finance-decision-${decisionData.id}`}
      opaque={false}
      open={open}
      toggleOpen={toggleOpen}
      title={
        <H3
          noMargin
          data-qa="finance-decision-title"
          aria-label={`${
            t.decisions.financeDecisions.type[decisionData.type]
          } ${formatFullDuration()}`}
        >
          {t.decisions.financeDecisions.type[decisionData.type]}{' '}
          {` ${formatTitleDuration()}`}
        </H3>
      }
      paddingHorizontal="0"
      paddingVertical="0"
    >
      <Gap size="xs" />
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <Label>{t.decisions.financeDecisions.validityPeriod}</Label>
        <span data-qa="finance-decision-validity-period">
          {decisionData.validFrom.format()}
        </span>

        <Label>{t.decisions.financeDecisions.sentAt}</Label>
        <span data-qa="finance-decision-sent-at">
          {decisionData.sentAt.format()}
        </span>

        <Label>{t.decisions.financeDecisions.liableCitizens}</Label>
        <span data-qa="finance-decision-co-debtors" translate="no">
          {decisionData.coDebtors
            .map((citizen) => formatPersonName(citizen, 'First Last'))
            .join(', ')}
        </span>
        {decisionData.decisionChildren.length > 0 && (
          <>
            <Label>{t.decisions.financeDecisions.voucherValueChild}</Label>
            <span data-qa="finance-decision-children" translate="no">
              {decisionData.decisionChildren
                .map((child) => formatPersonName(child, 'First Last'))
                .join(', ')}
            </span>
          </>
        )}
      </ListGrid>
      <Gap size="m" />

      <Button
        appearance="inline"
        icon={faFileAlt}
        text={t.decisions.financeDecisions.loadDecisionPDF}
        onClick={() =>
          window.open(
            getDecisionUrl(decisionData.id, decisionData.type),
            '_blank',
            'noopener,noreferrer'
          )
        }
      />
      {featureFlags.showMetadataToCitizen && (
        <>
          <Gap size="s" />
          <MetadataSection
            data-qa={decisionData.id}
            query={getMetadataQuery(decisionData.id, decisionData.type)}
          />
        </>
      )}
    </CollapsibleContentArea>
  )
})
