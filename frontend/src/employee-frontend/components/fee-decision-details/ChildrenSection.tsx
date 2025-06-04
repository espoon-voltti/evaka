// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import styled from 'styled-components'

import type { FeeDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { formatCents } from 'lib-common/money'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

interface Props {
  decision: FeeDecisionDetailed
}

export default React.memo(function ChildrenSection({ decision }: Props) {
  const { i18n } = useTranslation()

  return (
    <section>
      <H3>{i18n.feeDecision.form.summary.parts.title}</H3>
      {decision.children.map(
        ({
          child,
          placementType,
          serviceNeedFeeCoefficient,
          serviceNeedDescriptionFi,
          fee,
          siblingDiscount,
          feeAlterations,
          finalFee
        }) => {
          const mainDescription = `${
            i18n.placement.type[placementType]
          }, ${serviceNeedDescriptionFi.toLowerCase()} (${
            serviceNeedFeeCoefficient * 100
          } %)${
            siblingDiscount
              ? `, ${i18n.feeDecision.form.summary.parts.siblingDiscount} ${siblingDiscount}%`
              : ''
          }`

          return (
            <Part key={child.id}>
              <H4 noMargin>
                <PersonName person={child} format="First Last" />
              </H4>
              <Gap size="xs" />
              <PartRow>
                <span>{mainDescription}</span>
                <b>{`${formatCents(fee) ?? ''} €`}</b>
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
                <b>{formatCents(finalFee)} €</b>
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
