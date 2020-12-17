// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { faEuroSign } from '~icon-set'
import { Gap } from '@evaka/lib-components/src/white-space'
import CollapsibleSection from '~components/shared/molecules/CollapsibleSection'
import { H4 } from '~components/shared/Typography'
import VoucherValueDecisionIncomeSection from './VoucherValueDecisionIncomeSection'
import VoucherValueDecisionCoPaymentSection from './VoucherValueDecisionCoPaymentSection'
import VoucherValueDecisionValueSection from './VoucherValueDecisionValueSection'
import { useTranslation } from '~state/i18n'
import { VoucherValueDecisionDetailed } from '~types/invoicing'
import { formatCents } from '~utils/money'

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
      <Total>
        <TotalTitle noMargin>
          {i18n.valueDecision.summary.totalCoPayment}
        </TotalTitle>
        <b>{formatCents(decision.totalCoPayment)} €</b>
      </Total>
      <Gap size="m" />
      <VoucherValueDecisionValueSection decision={decision} />
      <Gap size="m" />
      <Total>
        <TotalTitle noMargin>
          {i18n.valueDecision.summary.totalValue}
        </TotalTitle>
        <b>{formatCents(decision.totalValue)} €</b>
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
  font-weight: 600;
`
