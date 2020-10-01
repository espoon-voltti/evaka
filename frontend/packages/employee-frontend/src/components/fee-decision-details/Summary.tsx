// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { faEuroSign } from '@evaka/icons'
import { Collapsible, Title } from '~components/shared/alpha'
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
  const [toggled, setToggled] = useState(true)

  return (
    <Collapsible
      title={i18n.feeDecision.form.summary.title}
      icon={faEuroSign}
      open={toggled}
      onToggle={() => setToggled((prev) => !prev)}
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
    </Collapsible>
  )
})

export default Summary
