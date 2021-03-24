// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'
import { formatCents } from '../../utils/money'
import { formatName } from '../../utils'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function VoucherValueDecisionValueSection({
  decision
}: Props) {
  const { i18n } = useTranslation()

  return (
    <section>
      <H3 noMargin>{i18n.valueDecision.summary.values}</H3>
      <Gap size="s" />
      {decision.parts.map(
        ({
          child,
          placement,
          childAge,
          ageCoefficient,
          serviceCoefficient,
          value
        }) => {
          const mainDescription = `${
            i18n.valueDecision.summary.age[
              childAge < 3 ? 'LESS_THAN_3' : 'OVER_3'
            ]
          } (${ageCoefficient} %), ${i18n.placement.type[
            placement.type
          ].toLowerCase()}${
            placement.hours
              ? `, ${placement.hours} ${i18n.valueDecision.summary.hoursPerWeek}`
              : ''
          } (${serviceCoefficient} %)`

          return (
            <Part key={child.id}>
              <H4 noMargin>
                {formatName(child.firstName, child.lastName, i18n)}
              </H4>
              <Gap size="xs" />
              <PartRow>
                <span>{mainDescription}</span>
                <b>{`${formatCents(value) ?? ''} €`}</b>
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
