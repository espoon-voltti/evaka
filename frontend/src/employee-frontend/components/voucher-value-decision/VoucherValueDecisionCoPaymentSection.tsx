// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'

import type { VoucherValueDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function VoucherValueDecisionCoPaymentSection({
  decision: {
    placement,
    serviceNeed,
    siblingDiscount,
    coPayment,
    feeAlterations,
    finalCoPayment
  }
}: Props) {
  const { i18n } = useTranslation()

  const mainDescription = `${i18n.placement.type[placement.type]}, ${
    serviceNeed.feeDescriptionFi
  } (${serviceNeed.feeCoefficient * 100} %)${
    siblingDiscount
      ? `, ${i18n.valueDecision.summary.siblingDiscount} ${siblingDiscount}%`
      : ''
  }`

  return (
    <section>
      <H3 noMargin>{i18n.valueDecision.summary.coPayment}</H3>
      <Gap size="s" />
      <Part>
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
        {feeAlterations.length > 0 ? (
          <PartRow>
            <b>{i18n.valueDecision.summary.sum}</b>
            <b>{formatCents(finalCoPayment)} €</b>
          </PartRow>
        ) : null}
      </Part>
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
  margin: 0 30px;
`
