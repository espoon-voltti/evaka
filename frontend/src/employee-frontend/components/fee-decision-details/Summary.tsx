// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { FeeDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { fontWeights, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faEuroSign } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import ChildrenSection from './ChildrenSection'
import IncomeSection from './IncomeSection'

interface Props {
  decision: FeeDecisionDetailed
}

export default React.memo(function Summary({ decision }: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={i18n.feeDecision.form.summary.title}
      icon={faEuroSign}
      startCollapsed={false}
      className="income-summary"
    >
      <IncomeSection decision={decision} />
      <Gap size="m" />
      <ChildrenSection decision={decision} />
      <Gap size="m" />
      <Total data-qa="decision-summary-total-price">
        <TotalTitle noMargin>
          {i18n.feeDecision.form.summary.totalPrice}
        </TotalTitle>
        <b>{formatCents(decision.totalFee)} €</b>
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
