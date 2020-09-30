// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Section, Title } from '~components/shared/alpha'
import { useTranslation } from '../../state/i18n'
import { FeeDecisionDetailed } from '../../types/invoicing'
import { formatCents } from '../../utils/money'
import { formatName } from '~utils'

interface Props {
  decision: FeeDecisionDetailed
}

function PartsSection({ decision }: Props) {
  const { i18n } = useTranslation()

  return (
    <Section>
      <Title size={3}>{i18n.feeDecision.form.summary.parts.title}</Title>
      {decision.parts.map(
        ({
          child,
          placement,
          fee: price,
          siblingDiscount,
          serviceNeedMultiplier,
          feeAlterations,
          finalFee: finalPrice
        }) => {
          const mainDescription = `${
            i18n.placement.type[placement.type]
          }, ${i18n.feeDecision.form.summary.part.serviceNeedAmount(
            placement.type,
            placement.serviceNeed
          )} (${serviceNeedMultiplier} %)${
            siblingDiscount
              ? `, ${i18n.feeDecision.form.summary.parts.siblingDiscount} ${siblingDiscount}%`
              : ''
          }`

          return (
            <div key={child.id} className="part">
              <Title size={4}>
                {formatName(child.firstName, child.lastName, i18n)}
              </Title>
              <div className="part-row">
                <div>{mainDescription}</div>
                <div>
                  <b>{`${formatCents(price) ?? ''} €`}</b>
                </div>
              </div>
              {feeAlterations.map((feeAlteration, index) => (
                <div key={index} className="part-row">
                  <div>{`${i18n.feeAlteration[feeAlteration.type]} ${
                    feeAlteration.amount
                  }${feeAlteration.isAbsolute ? '€' : '%'}`}</div>
                  <div>
                    <b>{`${formatCents(feeAlteration.effect) ?? ''} €`}</b>
                  </div>
                </div>
              ))}
              <div className="part-row">
                <div>
                  <b>{i18n.feeDecision.form.summary.parts.sum}</b>
                </div>
                <div>
                  <b>{formatCents(finalPrice)} €</b>
                </div>
              </div>
            </div>
          )
        }
      )}
    </Section>
  )
}

export default PartsSection
