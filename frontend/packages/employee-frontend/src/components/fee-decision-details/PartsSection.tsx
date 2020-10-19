// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'
import Section from '~components/shared/layout/Section'
import { Gap } from '~components/shared/layout/white-space'
import { H3, H4 } from '~components/shared/Typography'
import { useTranslation } from '~state/i18n'
import { FeeDecisionDetailed } from '~types/invoicing'
import { formatCents } from '~utils/money'
import { formatName } from '~utils'

interface Props {
  decision: FeeDecisionDetailed
}

export default React.memo(function PartsSection({ decision }: Props) {
  const { i18n } = useTranslation()

  return (
    <Section>
      <H3>{i18n.feeDecision.form.summary.parts.title}</H3>
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
          }, ${i18n.placement.serviceNeed[
            placement.serviceNeed
          ].toLowerCase()} (${serviceNeedMultiplier} %)${
            siblingDiscount
              ? `, ${i18n.feeDecision.form.summary.parts.siblingDiscount} ${siblingDiscount}%`
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
                <b>{`${formatCents(price) ?? ''} €`}</b>
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
                <b>{i18n.feeDecision.form.summary.parts.sum}</b>
                <b>{formatCents(finalPrice)} €</b>
              </PartRow>
            </Part>
          )
        }
      )}
    </Section>
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
