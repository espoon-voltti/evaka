// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { VoucherValueDecisionDetailed } from '../../types/invoicing'
import { formatCents } from '../../utils/money'

type Props = {
  decision: VoucherValueDecisionDetailed
}

export default React.memo(function VoucherValueDecisionValueSection({
  decision: { childAge, ageCoefficient, placement, serviceCoefficient, value }
}: Props) {
  const { i18n } = useTranslation()

  const mainDescription = `${
    i18n.valueDecision.summary.age[childAge < 3 ? 'LESS_THAN_3' : 'OVER_3']
  } (${ageCoefficient} %), ${i18n.placement.type[
    placement.type
  ].toLowerCase()}${
    placement.hours
      ? `, ${placement.hours} ${i18n.valueDecision.summary.hoursPerWeek}`
      : ''
  } (${serviceCoefficient} %)`

  return (
    <section>
      <H3 noMargin>{i18n.valueDecision.summary.value}</H3>
      <Gap size="s" />
      <PartRow>
        <span>{mainDescription}</span>
        <b>{`${formatCents(value) ?? ''} €`}</b>
      </PartRow>
    </section>
  )
})

const PartRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin: 0 30px;
`
