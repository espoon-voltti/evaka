// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { faEuroSign } from 'icon-set'
import CollapsibleSection from '~components/shared/molecules/CollapsibleSection'
import Title from '~components/shared/atoms/Title'
import IncomeSection from './IncomeSection'
import PartsSection from './PartsSection'
import { useTranslation } from '../../state/i18n'
import { FeeDecisionDetailed } from '../../types/invoicing'
import { formatCents } from '../../utils/money'

import './Summary.scss'

interface Props {
  decision: FeeDecisionDetailed
}

const Summary = React.memo(function Summary({ decision }: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={i18n.feeDecision.form.summary.title}
      icon={faEuroSign}
      startCollapsed={false}
      className="income-summary"
    >
      <IncomeSection decision={decision} />
      <PartsSection decision={decision} />
      <div className="total-price" data-qa="decision-summary-total-price">
        <div>
          <Title size={3}>
            <b>{i18n.feeDecision.form.summary.totalPrice}</b>
          </Title>
        </div>
        <div>
          <b>{formatCents(decision.totalFee)} €</b>
        </div>
      </div>
    </CollapsibleSection>
  )
})

export default Summary
