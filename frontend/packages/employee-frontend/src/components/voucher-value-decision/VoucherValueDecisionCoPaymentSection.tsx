// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'
import { H3, H4 } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '../../state/i18n'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'
import { formatCents } from '../../utils/money'
import { formatName } from '../../utils'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function VoucherValueDecisionCoPaymentSection({
  decision
}: Props) {
  const { i18n } = useTranslation()

  return (
    <section>
      <H3 noMargin>{i18n.valueDecision.summary.parts}</H3>
      <Gap size="s" />
      {decision.parts.map(
        ({
          child,
          placement,
          coPayment,
          siblingDiscount,
          serviceNeedMultiplier,
          feeAlterations,
          finalCoPayment
        }) => {
          const mainDescription = `${
            i18n.placement.type[placement.type]
          }, ${i18n.placement.serviceNeed[
            placement.serviceNeed
          ].toLowerCase()} (${serviceNeedMultiplier} %)${
            siblingDiscount
              ? `, ${i18n.valueDecision.summary.siblingDiscount} ${siblingDiscount}%`
              : ''
          }`

          return (
            <Part key={child.id}>
              <H4 noMargin>
                {formatName(child.firstName, child.lastName, i18n)}
              </H4>
              <Gap size="xs" />
              <PartRow>
                <span>{mainDescription}</span>
                <b>{`${formatCents(coPayment) ?? ''} €`}</b>
              </PartRow>
              <Gap size="xs" />
              {feeAlterations.map((feeAlteration, index) => (
                <Fragment key={index}>
                  <PartRow>
                    <span>{`${i18n.feeAlteration[feeAlteration.type]} ${
                      feeAlteration.amount
                    }${feeAlteration.isAbsolute ? '€' : '%'}`}</span>
                    <b>{`${formatCents(feeAlteration.effect) ?? ''} €`}</b>
                  </PartRow>
                  <Gap size="xs" />
                </Fragment>
              ))}
              <PartRow>
                <b>{i18n.valueDecision.summary.sum}</b>
                <b>{formatCents(finalCoPayment)} €</b>
              </PartRow>
            </Part>
          )
        }
      )}
    </section>
  )
})

const Part = styled.div`
  display: flex;
  flex-direction: column;
`

const PartRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin-left: 5vw;
  margin-right: 30px;
`
