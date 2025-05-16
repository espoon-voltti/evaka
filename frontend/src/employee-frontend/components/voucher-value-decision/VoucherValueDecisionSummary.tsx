// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { VoucherValueDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { fontWeights, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faEuroSign } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import VoucherValueDecisionCoPaymentSection from './VoucherValueDecisionCoPaymentSection'
import VoucherValueDecisionIncomeSection from './VoucherValueDecisionIncomeSection'
import VoucherValueDecisionValueSection from './VoucherValueDecisionValueSection'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function Summary({ decision }: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={i18n.valueDecision.summary.title}
      icon={faEuroSign}
      startCollapsed={false}
    >
      <VoucherValueDecisionIncomeSection decision={decision} />
      <Gap size="m" />
      <VoucherValueDecisionCoPaymentSection decision={decision} />
      <Gap size="m" />
      <VoucherValueDecisionValueSection decision={decision} />
      <Gap size="m" />
      <Total>
        <TotalTitle noMargin>
          {i18n.valueDecision.summary.totalValue}
        </TotalTitle>
        <b>{formatCents(decision.voucherValue - decision.finalCoPayment)} €</b>
      </Total>
    </CollapsibleSection>
  )
})

const Total = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  background: ghostwhite;
  padding: 30px;
`

const TotalTitle = styled(H4)`
  font-weight: ${fontWeights.semibold};
`
